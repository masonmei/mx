package com.newrelic.agent.instrumentation;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.InvocationHandler;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassWriter;

import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.agent.Agent;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.TracerService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.errors.ErrorService;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcherBuilder;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.ContextClassTransformer;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import com.newrelic.agent.util.Annotations;
import com.newrelic.agent.util.Invoker;

public class ClassTransformer implements ContextClassTransformer {
    protected final Collection<PointCut> pointcuts;
    protected final ClassNameFilter classNameFilter;
    private final int classreaderFlags;
    private final InstrumentationProxy instrumentation;
    private final boolean retransformSupported;
    private final IAgentLogger logger;
    private final ClassMatchVisitorFactory matcher;

    protected ClassTransformer(InstrumentationProxy pInstrumentation, boolean pRetransformSupported) {
        instrumentation = pInstrumentation;
        logger = Agent.LOG.getChildLogger(ClassTransformer.class);
        initAgentHandle();
        classNameFilter = new ClassNameFilter(logger);
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        classNameFilter.addConfigClassFilters(config);
        classNameFilter.addExcludeFileClassFilters();

        classreaderFlags = instrumentation.getClassReaderFlags();
        retransformSupported = pRetransformSupported;

        List<PointCut> pcs = new LinkedList(findEnabledPointCuts());
        pcs.addAll(ErrorService.getEnabledErrorHandlerPointCuts());

        Collections.sort(pcs);
        pointcuts = Collections.unmodifiableCollection(pcs);

        setPointcutProperties();
        matcher = OptimizedClassMatcherBuilder.newBuilder().addClassMethodMatcher(pointcuts.toArray(new PointCut[0]))
                          .build();
    }

    public static boolean isInstrumented(Class<?> clazz) {
        if (clazz.getAnnotation(InstrumentedClass.class) != null) {
            return true;
        }
        return false;
    }

    public static boolean isInstrumentedAndModified(Class<?> clazz) {
        if (clazz.getAnnotation(InstrumentedClass.class) != null) {
            return (clazz.getAnnotation(InstrumentedClass.class)).classStructureModified();
        }
        return false;
    }

    public static boolean canModifyClassStructure(ClassLoader classLoader, Class<?> classBeingRedefined) {
        return !hasBeenLoaded(classBeingRedefined) || isInstrumentedAndModified(classBeingRedefined);
    }

    public static boolean hasBeenLoaded(Class<?> clazz) {
        return null != clazz;
    }

    public ClassMatchVisitorFactory getMatcher() {
        return matcher;
    }

    private void setPointcutProperties() {
        List handlers = new ArrayList(pointcuts.size());
        Collection classMatchers = new ArrayList();
        for (PointCut pc : pointcuts) {
            handlers.add(pc.getPointCutInvocationHandler());
            classMatchers.add(pc.getClassMatcher());
        }
        classNameFilter.addClassMatcherIncludes(classMatchers);
        ServiceFactory.getTracerService().registerInvocationHandlers(handlers);

        logger.finer("A Class transformer is initialized");
    }

    private void initAgentHandle() {
        com.newrelic.agent.bridge.AgentBridge.agentHandler = AgentWrapper.getAgentWrapper(this);
    }

    Collection<PointCut> findEnabledPointCuts() {
        Collection<Class<?>> classes = Annotations
                                               .getAnnotationClassesFromManifest(com.newrelic.agent.instrumentation
                                                                                         .pointcuts.PointCut.class,
                                                                                        "com/newrelic/agent/instrumentation/pointcuts");

        Collection pointcuts = new ArrayList();
        for (Class clazz : classes) {
            PointCut pc = createPointCut(clazz);
            if (pc.isEnabled()) {
                pointcuts.add(pc);
            }
        }

        return pointcuts;
    }

    private PointCut createPointCut(Class<PointCut> clazz) {
        try {
            return (PointCut) clazz.getConstructor(new Class[] {ClassTransformer.class})
                                      .newInstance(new Object[] {this});
        } catch (Exception e) {
            String msg = MessageFormat.format("Unable to create pointcut {0} : {1}",
                                                     new Object[] {clazz.getName(), e.toString()});

            Agent.LOG.severe(msg);
            Agent.LOG.log(Level.FINE, msg, e);
        }
        return null;
    }

    public Collection<PointCut> getPointcuts() {
        return pointcuts;
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer, InstrumentationContext context,
                            OptimizedClassMatcher.Match match) throws IllegalClassFormatException {
        if (!shouldTransform(loader, className, classfileBuffer)) {
            logger.trace(MessageFormat.format("Skipped instrumenting {0}", new Object[] {className}));
            return null;
        }
        try {
            WeavingLoaderImpl weavingLoader = getWeavingLoader(loader);
            if ((Agent.isDebugEnabled()) && (logger.isTraceEnabled())) {
                logger.trace(MessageFormat.format("Considering instrumenting {0}", new Object[] {className}));
            }
            return weavingLoader.preProcess(context, className, classBeingRedefined, classfileBuffer, match);
        } catch (ThreadDeath e) {
            throw e;
        } catch (Throwable e) {
            logger.severe(MessageFormat.format("An error occurred processing class {0} : {1}",
                                                      new Object[] {className, e.toString()}));
            if (Agent.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
        return null;
    }

    protected boolean shouldTransform(ClassLoader loader, String className, byte[] classfileBuffer) {
        boolean isLoggable = (Agent.isDebugEnabled()) && (logger.isLoggable(Level.FINEST));

        if (isIncluded(className)) {
            if (isLoggable) {
                logger.finest(MessageFormat.format("Class {0} is explicitly included", new Object[] {className}));
            }
            return true;
        }

        if (isExcluded(className)) {
            if (isLoggable) {
                logger.finest(MessageFormat
                                      .format("Skipping class {0} because it is excluded", new Object[] {className}));
            }
            return false;
        }

        if (className.startsWith("$")) {
            if (isLoggable) {
                logger.finest(MessageFormat
                                      .format("Skipping class {0} because it starts with $", new Object[] {className}));
            }
            return false;
        }

        if ((className.indexOf("$$") > 0) && (!className.startsWith("play"))) {
            if (isLoggable) {
                logger.finest(MessageFormat.format("Skipping class {0} because it contains $$ and is not a Play class",
                                                          new Object[] {className}));
            }

            return false;
        }

        if (isValidClassByteArray(classfileBuffer)) {
            if (isLoggable) {
                logger.finest(MessageFormat
                                      .format("Skipping class {0} because it does not appear to be a valid class file",
                                                     new Object[] {className}));
            }

            return false;
        }

        if ((loader == null) && (!isBootstrapClassInstrumentationEnabled())) {
            if (isLoggable) {
                logger.finest(MessageFormat.format("Skipping class {0} because bootstrap class instrumentation is not "
                                                           + "supported", new Object[] {className}));
            }

            return false;
        }

        return true;
    }

    private boolean isBootstrapClassInstrumentationEnabled() {
        return instrumentation.isBootstrapClassInstrumentationEnabled();
    }

    protected boolean isIncluded(String className) {
        return classNameFilter.isIncluded(className);
    }

    protected boolean isExcluded(String className) {
        return classNameFilter.isExcluded(className);
    }

    protected boolean isRetransformSupported() {
        return retransformSupported;
    }

    private boolean isValidClassByteArray(byte[] classfileBuffer) {
        return (classfileBuffer.length >= 4) && (classfileBuffer[0] == -54) && (classfileBuffer[0] == -2)
                       && (classfileBuffer[0] == -70) && (classfileBuffer[0] == -66);
    }

    protected int getClassReaderFlags() {
        return classreaderFlags;
    }

    protected WeavingLoaderImpl getWeavingLoader(ClassLoader loader) {
        return new WeavingLoaderImpl(loader);
    }

    protected WeavingLoaderImpl getWeavingLoader(ClassLoader loader, boolean pIsRetrans) {
        return new WeavingLoaderImpl(loader);
    }

    InstrumentationProxy getInstrumentation() {
        return instrumentation;
    }

    public final ClassNameFilter getClassNameFilter() {
        return classNameFilter;
    }

    public InvocationHandler evaluate(Class clazz, TracerService tracerService, Object className, Object methodName,
                                      Object methodDesc, boolean ignoreApdex, Object[] args) {
        ClassMethodSignature classMethodSignature =
                new ClassMethodSignature(((String) className).replace('/', '.'), (String) methodName,
                                                (String) methodDesc);

        for (PointCut pc : getPointcuts()) {
            if ((pc.getClassMatcher().isMatch(clazz)) && (pc.getMethodMatcher()
                                                                  .matches(-1, classMethodSignature.getMethodName(),
                                                                                  classMethodSignature.getMethodDesc(),
                                                                                  MethodMatcher
                                                                                          .UNSPECIFIED_ANNOTATIONS))) {
                PointCutInvocationHandler invocationHandler = pc.getPointCutInvocationHandler();
                return InvocationPoint
                               .getInvocationPoint(invocationHandler, tracerService, classMethodSignature, ignoreApdex);
            }
        }

        if (ignoreApdex) {
            return IgnoreApdexInvocationHandler.INVOCATION_HANDLER;
        }

        Agent.LOG.log(Level.FINE, "No invocation handler was registered for {0}", new Object[] {classMethodSignature});

        return NoOpInvocationHandler.INVOCATION_HANDLER;
    }

    class WeavingLoaderImpl {
        private final ClassLoader classLoader;

        public WeavingLoaderImpl(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        public byte[] preProcess(InstrumentationContext context, String className, Class<?> classBeingRedefined,
                                 byte[] classfileBuffer, OptimizedClassMatcher.Match match) {
            ClassReader cr = new ClassReader(classfileBuffer);
            if (InstrumentationUtils.isInterface(cr)) {
                return null;
            }

            Collection strongMatches = Lists.newArrayList(pointcuts);
            strongMatches.retainAll(match.getClassMatches().keySet());

            if (strongMatches.isEmpty()) {
                return null;
            }

            if ((classLoader != null) && (!InstrumentationUtils.isAbleToResolveAgent(classLoader, className))) {
                String msg = MessageFormat.format("Not instrumenting {0}: class loader unable to load agent classes",
                                                         className);

                Agent.LOG.log(Level.FINER, msg);
                return null;
            }
            try {
                if (ClassTransformer.canModifyClassStructure(classLoader, classBeingRedefined)) {
                    byte[] classfileBufferWithUID = InstrumentationUtils.generateClassBytesWithSerialVersionUID(cr,
                                                                                                                       classreaderFlags,
                                                                                                                       classLoader);

                    cr = new ClassReader(classfileBufferWithUID);
                }

                ClassWriter cw = InstrumentationUtils.getClassWriter(cr, classLoader);

                GenericClassAdapter adapter =
                        new GenericClassAdapter(cw, classLoader, className, classBeingRedefined, strongMatches,
                                                       context);

                cr.accept(adapter, classreaderFlags);

                if (adapter.getInstrumentedMethods().size() > 0) {
                    if (Agent.LOG.isFinerEnabled()) {
                        String msg = MessageFormat.format("Instrumenting {0}", new Object[] {className});
                        Agent.LOG.finer(msg);
                    }

                    return cw.toByteArray();
                }

                return null;
            } catch (StopProcessingException e) {
                return null;
            } catch (ArrayIndexOutOfBoundsException t) {
                String msg = MessageFormat
                                     .format("Skipping transformation of class {0} ({1} bytes) because an ASM array "
                                                     + "bounds exception occurred: {2}",
                                                    new Object[] {className, Integer.valueOf(classfileBuffer.length),
                                                                         t.toString()});

                logger.warning(msg);
                if (logger.isLoggable(Level.FINER)) {
                    msg = MessageFormat.format("ASM error for pointcut(s) : strong {0}", new Object[] {strongMatches});
                    logger.finer(msg);
                    logger.log(Level.FINER, "ASM error", t);
                }
                if (Boolean.getBoolean("newrelic.asm.error.stop")) {
                    System.exit(-1);
                }
                return null;
            } catch (ThreadDeath e) {
                throw e;
            } catch (Throwable t) {
                logger.warning(MessageFormat
                                       .format("Skipping transformation of class {0} because an error occurred: {1}",
                                                      new Object[] {className, t.toString()}));

                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "Error transforming class " + className, t);
                }
            }
            return null;
        }

        private boolean isAbleToResolveAgent(ClassLoader loader) {
            try {
                ClassLoaderCheck.loadAgentClass(loader);
                return true;
            } catch (Throwable t) {
                String msg = MessageFormat
                                     .format("Classloader {0} failed to load Agent class. The agent might need to be "
                                                     + "loaded by the bootstrap classloader.: {1}",
                                                    new Object[] {loader.getClass().getName(), t});

                if (Agent.LOG.isLoggable(Level.FINEST)) {
                    Agent.LOG.log(Level.FINEST, msg, t);
                } else if (Agent.LOG.isLoggable(Level.FINER)) {
                    Agent.LOG.finer(msg);
                }
            }
            return false;
        }

        private synchronized void redefineClass(String className, byte[] classfileBuffer) {
            try {
                instrumentation.redefineClasses(new ClassDefinition[] {new ClassDefinition(classLoader
                                                                                                   .loadClass(Invoker.getClassNameFromInternalName(className)),
                                                                                                  classfileBuffer)});
            } catch (ClassNotFoundException e) {
                String msg = MessageFormat.format("An error occurred redefining class {0}: {1}",
                                                         new Object[] {className, e});
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, msg, e);
                } else if (logger.isLoggable(Level.FINER)) {
                    logger.finer(msg);
                }
            } catch (UnmodifiableClassException e) {
                String msg = MessageFormat.format("An error occurred redefining class {0}: {1}",
                                                         new Object[] {className, e});
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, msg, e);
                } else if (logger.isLoggable(Level.FINER)) {
                    logger.finer(msg);
                }
            }
        }

        public ClassLoader getClassLoader() {
            return classLoader;
        }
    }
}