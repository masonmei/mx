package com.newrelic.agent.instrumentation;

import java.io.Closeable;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.logging.Level;

import org.objectweb.asm.Type;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.TransactionApiImpl;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.agent.instrumentation.classmatchers.DefaultClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.HashSafeClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.AccessMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.AndMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.GetterSetterMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.NotMethodMatcher;
import com.newrelic.agent.instrumentation.weaver.WeaveInstrumentation;
import com.newrelic.agent.reinstrument.PeriodicRetransformer;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ClassMethodSignatures;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.MetricNameFormats;
import com.newrelic.agent.util.InsertOnlyArray;
import com.newrelic.api.agent.Logger;

public class InstrumentationImpl implements Instrumentation {
    private final Logger logger;
    private final InsertOnlyArray<Object> objectCache = new InsertOnlyArray(16);

    private final Set<Type> weaveClasses = Sets.newSetFromMap(Maps.<Type, Boolean>newConcurrentMap());

    public InstrumentationImpl(Logger logger) {
        this.logger = logger;
    }

    public ExitTracer createTracer(Object invocationTarget, int signatureId, boolean dispatcher, String metricName,
                                   String tracerFactoryName, Object[] args) {
        com.newrelic.agent.Transaction transaction = com.newrelic.agent.Transaction.getTransaction();
        if (transaction == null) {
            return null;
        }
        if (ServiceFactory.getServiceManager().getCircuitBreakerService().isTripped()) {
            return null;
        }
        try {
            if ((!dispatcher) && (!transaction.isStarted()) && (tracerFactoryName == null)) {
                return null;
            }

            if (transaction.getTransactionActivity().isFlyweight()) {
                return null;
            }

            ClassMethodSignature sig = ClassMethodSignatures.get().get(signatureId);
            return transaction.getTransactionState()
                           .getTracer(transaction, tracerFactoryName, sig, invocationTarget, args);
        } catch (Throwable t) {
            logger.log(Level.FINEST, t, "createTracer({0}, {1}, {2})",
                              new Object[] {invocationTarget, Integer.valueOf(signatureId), metricName});
        }
        return null;
    }

    public ExitTracer createTracer(Object invocationTarget, int signatureId, String metricName, int flags) {
        try {
            if (ServiceFactory.getServiceManager().getCircuitBreakerService().isTripped()) {
                return null;
            }
            if (!Agent.canFastPath()) {
                return oldCreateTracer(invocationTarget, signatureId, metricName, flags);
            }

            TransactionActivity txa = TransactionActivity.get();
            if (txa == null) {
                if (!TracerFlags.isDispatcher(flags)) {
                    return null;
                }

                com.newrelic.agent.Transaction tx = com.newrelic.agent.Transaction.getTransaction();
                ClassMethodSignature sig = ClassMethodSignatures.get().get(signatureId);
                return tx.getTransactionState().getTracer(tx, invocationTarget, sig, metricName, flags);
            }

            if ((!TracerFlags.isDispatcher(flags)) && (!txa.isStarted())) {
                return null;
            }

            Tracer result = null;
            if (txa.checkTracerStart()) {
                try {
                    ClassMethodSignature sig = ClassMethodSignatures.get().get(signatureId);

                    MetricNameFormat mnf = MetricNameFormats.getFormatter(invocationTarget, sig, metricName, flags);
                    if (TracerFlags.isDispatcher(flags)) {
                        result = new OtherRootTracer(txa, sig, invocationTarget, mnf);
                    } else {
                        result = new DefaultTracer(txa, sig, invocationTarget, mnf, flags);
                    }
                } finally {
                    txa.unlockTracerStart();
                }

                txa.tracerStarted(result);
            }
            return result;
        } catch (Throwable t) {
            logger.log(Level.FINEST, t, "createTracer({0}, {1}, {2}, {3})",
                              new Object[] {invocationTarget, Integer.valueOf(signatureId), metricName,
                                                   Integer.valueOf(flags)});
        }
        return null;
    }

    private ExitTracer oldCreateTracer(Object invocationTarget, int signatureId, String metricName, int flags) {
        com.newrelic.agent.Transaction transaction = com.newrelic.agent.Transaction.getTransaction();
        if (transaction == null) {
            return null;
        }
        try {
            if ((!TracerFlags.isDispatcher(flags)) && (!transaction.isStarted())) {
                return null;
            }
            if (transaction.getTransactionActivity().isFlyweight()) {
                return null;
            }
            ClassMethodSignature sig = ClassMethodSignatures.get().get(signatureId);
            return transaction.getTransactionState().getTracer(transaction, invocationTarget, sig, metricName, flags);
        } catch (Throwable t) {
            logger.log(Level.FINEST, t, "createTracer({0}, {1}, {2}, {3})",
                              new Object[] {invocationTarget, Integer.valueOf(signatureId), metricName,
                                                   Integer.valueOf(flags)});
        }
        return null;
    }

    public void noticeInstrumentationError(Throwable throwable, String libraryName) {
        if (Agent.LOG.isFinerEnabled()) {
            logger.log(Level.FINER, "An error was thrown from instrumentation library ", new Object[] {libraryName});
            logger.log(Level.FINEST, throwable, "An error was thrown from instrumentation library ",
                              new Object[] {libraryName});
        }
    }

    public void instrument(String className, String metricPrefix) {
        DefaultClassAndMethodMatcher matcher = new HashSafeClassAndMethodMatcher(new ExactClassMatcher(className),
                                                                                        AndMethodMatcher
                                                                                                .getMethodMatcher(new MethodMatcher[] {new AccessMethodMatcher(1),
                                                                                                                                              new NotMethodMatcher(GetterSetterMethodMatcher
                                                                                                                                                                           .getGetterSetterMethodMatcher())}));

        ServiceFactory.getClassTransformerService().addTraceMatcher(matcher, metricPrefix);
    }

    public void instrument(java.lang.reflect.Method methodToInstrument, String metricPrefix) {
        if (methodToInstrument.isAnnotationPresent(InstrumentedMethod.class)) {
            return;
        }
        if (OptimizedClassMatcher.METHODS_WE_NEVER_INSTRUMENT
                    .contains(org.objectweb.asm.commons.Method.getMethod(methodToInstrument))) {
            return;
        }
        int modifiers = methodToInstrument.getModifiers();
        if ((Modifier.isNative(modifiers)) || (Modifier.isAbstract(modifiers))) {
            return;
        }

        Class declaringClass = methodToInstrument.getDeclaringClass();
        DefaultClassAndMethodMatcher matcher =
                new HashSafeClassAndMethodMatcher(new ExactClassMatcher(declaringClass.getName()),
                                                         new ExactMethodMatcher(methodToInstrument.getName(),
                                                                                       Type.getMethodDescriptor
                                                                                                    (methodToInstrument)));

        boolean shouldRetransform = ServiceFactory.getClassTransformerService().addTraceMatcher(matcher, metricPrefix);
        if (shouldRetransform) {
            logger.log(Level.FINE, "Retransforming {0} for instrumentation.", new Object[] {methodToInstrument});
            PeriodicRetransformer.INSTANCE.queueRetransform(declaringClass);
        }
    }

    public void retransformUninstrumentedClass(Class<?> classToRetransform) {
        if (!classToRetransform.isAnnotationPresent(InstrumentedClass.class)) {
            retransformClass(classToRetransform);
        } else {
            logger.log(Level.FINER, "Class ", new Object[] {classToRetransform, " already instrumented."});
        }
    }

    private void retransformClass(Class<?> classToRetransform) {
        try {
            ServiceFactory.getAgent().getInstrumentation().retransformClasses(new Class[] {classToRetransform});
        } catch (UnmodifiableClassException e) {
            logger.log(Level.FINE, "Unable to retransform class ",
                              new Object[] {classToRetransform, " : ", e.getMessage()});
        }
    }

    public Class<?> loadClass(ClassLoader classLoader, Class<?> theClass) throws ClassNotFoundException {
        logger.log(Level.FINE, "Loading class ",
                          new Object[] {theClass.getName(), " using class loader ", classLoader.toString()});
        try {
            return classLoader.loadClass(theClass.getName());
        } catch (ClassNotFoundException e) {
            logger.log(Level.FINEST, "Unable to load",
                              new Object[] {theClass.getName(), ".  Appending it to the classloader."});

            WeaveInstrumentation weaveInstrumentation =
                    (WeaveInstrumentation) theClass.getAnnotation(WeaveInstrumentation.class);
            if (weaveInstrumentation != null) {
                logger.log(Level.FINE, theClass.getName(),
                                  new Object[] {" is defined in ", weaveInstrumentation.title(), " version ",
                                                       weaveInstrumentation.version()});
                try {
                    ServiceFactory.getClassTransformerService().getContextManager().getClassWeaverService()
                            .loadClass(classLoader, weaveInstrumentation.title(), theClass.getName());

                    return classLoader.loadClass(theClass.getName());
                } catch (Exception ex) {
                    throw new ClassNotFoundException("Unable to load " + theClass.getName()
                                                             + " from instrumentation package " + weaveInstrumentation
                                                                                                          .title());
                }
            }
        }

        throw new ClassNotFoundException("Unable to load " + theClass.getName());
    }

    public com.newrelic.agent.bridge.Transaction getTransaction() {
        try {
            com.newrelic.agent.Transaction innerTx = com.newrelic.agent.Transaction.getTransaction();
            if (innerTx != null) {
                return new TransactionApiImpl();
            }
        } catch (Throwable t) {
            logger.log(Level.FINE, t, "Unable to get transaction, using no-op transaction instead", new Object[0]);
        }

        return NoOpTransaction.INSTANCE;
    }

    public int addToObjectCache(Object object) {
        return objectCache.add(object);
    }

    public Object getCachedObject(int id) {
        return objectCache.get(id);
    }

    public boolean isWeaveClass(Class<?> clazz) {
        return weaveClasses.contains(Type.getType(clazz));
    }

    public void addWeaveClass(Type type) {
        weaveClasses.add(type);
    }

    public void registerCloseable(String instrumentationName, Closeable closeable) {
        if ((instrumentationName != null) && (closeable != null)) {
            ServiceFactory.getClassTransformerService().getContextManager().getClassWeaverService()
                    .registerInstrumentationCloseable(instrumentationName, closeable);
        }
    }
}