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

import com.newrelic.agent.Agent;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.TracerService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.errors.ErrorService;
import com.newrelic.agent.instrumentation.classmatchers.ClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher.Match;
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
import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassWriter;

public class ClassTransformer implements ContextClassTransformer {
    protected final Collection<PointCut> pointcuts;
    protected final ClassNameFilter classNameFilter;
    private final int classreaderFlags;
    private final InstrumentationProxy instrumentation;
    private final boolean retransformSupported;
    private final IAgentLogger logger;
    private final ClassMatchVisitorFactory matcher;

    protected ClassTransformer(InstrumentationProxy pInstrumentation, boolean pRetransformSupported) {
        this.instrumentation = pInstrumentation;
        this.logger = Agent.LOG.getChildLogger(ClassTransformer.class);
        initAgentHandle();
        this.classNameFilter = new ClassNameFilter(this.logger);
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        this.classNameFilter.addConfigClassFilters(config);
        this.classNameFilter.addExcludeFileClassFilters();

        this.classreaderFlags = this.instrumentation.getClassReaderFlags();
        this.retransformSupported = pRetransformSupported;

        List pcs = new LinkedList(findEnabledPointCuts());
        pcs.addAll(ErrorService.getEnabledErrorHandlerPointCuts());

        Collections.sort(pcs);
        this.pointcuts = Collections.unmodifiableCollection(pcs);

        setPointcutProperties();
        this.matcher = OptimizedClassMatcherBuilder.newBuilder()
                               .addClassMethodMatcher((ClassAndMethodMatcher[]) this.pointcuts.toArray(new PointCut[0]))
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
            return ((InstrumentedClass) clazz.getAnnotation(InstrumentedClass.class)).classStructureModified();
        }
        return false;
    }

    public static boolean canModifyClassStructure(ClassLoader classLoader, Class<?> classBeingRedefined) {
        if ((!hasBeenLoaded(classBeingRedefined)) || (isInstrumentedAndModified(classBeingRedefined))) {
            return true;
        }
        return false;
    }

    public static boolean hasBeenLoaded(Class<?> clazz) {
        return null != clazz;
    }

    public ClassMatchVisitorFactory getMatcher() {
        return this.matcher;
    }

    private void setPointcutProperties() {
        List handlers = new ArrayList(this.pointcuts.size());
        Collection classMatchers = new ArrayList();
        for (PointCut pc : this.pointcuts) {
            handlers.add(pc.getPointCutInvocationHandler());
            classMatchers.add(pc.getClassMatcher());
        }
        this.classNameFilter.addClassMatcherIncludes(classMatchers);
        ServiceFactory.getTracerService().registerInvocationHandlers(handlers);

        this.logger.finer("A Class transformer is initialized");
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
        return this.pointcuts;
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer, InstrumentationContext context,
                            Match match) throws IllegalClassFormatException {
        if (!shouldTransform(loader, className, classfileBuffer)) {
            this.logger.trace(MessageFormat.format("Skipped instrumenting {0}", new Object[] {className}));
            return null;
        }
        try {
            WeavingLoaderImpl weavingLoader = getWeavingLoader(loader);
            if ((Agent.isDebugEnabled()) && (this.logger.isTraceEnabled())) {
                this.logger.trace(MessageFormat.format("Considering instrumenting {0}", new Object[] {className}));
            }
            return weavingLoader.preProcess(context, className, classBeingRedefined, classfileBuffer, match);
        } catch (ThreadDeath e) {
            throw e;
        } catch (Throwable e) {
            this.logger.severe(MessageFormat.format("An error occurred processing class {0} : {1}",
                                                           new Object[] {className, e.toString()}));
            if (Agent.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
        return null;
    }

    protected boolean shouldTransform(ClassLoader loader, String className, byte[] classfileBuffer) {
        boolean isLoggable = (Agent.isDebugEnabled()) && (this.logger.isLoggable(Level.FINEST));

        if (isIncluded(className)) {
            if (isLoggable) {
                this.logger.finest(MessageFormat.format("Class {0} is explicitly included", new Object[] {className}));
            }
            return true;
        }

        if (isExcluded(className)) {
            if (isLoggable) {
                this.logger.finest(MessageFormat.format("Skipping class {0} because it is excluded",
                                                               new Object[] {className}));
            }
            return false;
        }

        if (className.startsWith("$")) {
            if (isLoggable) {
                this.logger.finest(MessageFormat.format("Skipping class {0} because it starts with $",
                                                               new Object[] {className}));
            }
            return false;
        }

        if ((className.indexOf("$$") > 0) && (!className.startsWith("play"))) {
            if (isLoggable) {
                this.logger.finest(MessageFormat
                                           .format("Skipping class {0} because it contains $$ and is not a Play class",
                                                          new Object[] {className}));
            }

            return false;
        }

        if (isValidClassByteArray(classfileBuffer)) {
            if (isLoggable) {
                this.logger.finest(MessageFormat
                                           .format("Skipping class {0} because it does not appear to be a valid class"
                                                           + " file", new Object[] {className}));
            }

            return false;
        }

        if ((loader == null) && (!isBootstrapClassInstrumentationEnabled())) {
            if (isLoggable) {
                this.logger.finest(MessageFormat
                                           .format("Skipping class {0} because bootstrap class instrumentation is not"
                                                           + " supported", new Object[] {className}));
            }

            return false;
        }

        return true;
    }

    private boolean isBootstrapClassInstrumentationEnabled() {
        return this.instrumentation.isBootstrapClassInstrumentationEnabled();
    }

    protected boolean isIncluded(String className) {
        return this.classNameFilter.isIncluded(className);
    }

    protected boolean isExcluded(String className) {
        return this.classNameFilter.isExcluded(className);
    }

    protected boolean isRetransformSupported() {
        return this.retransformSupported;
    }

    private boolean isValidClassByteArray(byte[] classfileBuffer) {
        return (classfileBuffer.length >= 4) && (classfileBuffer[0] == -54) && (classfileBuffer[0] == -2)
                       && (classfileBuffer[0] == -70) && (classfileBuffer[0] == -66);
    }

    protected int getClassReaderFlags() {
        return this.classreaderFlags;
    }

    protected WeavingLoaderImpl getWeavingLoader(ClassLoader loader) {
        return new WeavingLoaderImpl(loader);
    }

    protected WeavingLoaderImpl getWeavingLoader(ClassLoader loader, boolean pIsRetrans) {
        return new WeavingLoaderImpl(loader);
    }

    InstrumentationProxy getInstrumentation() {
        return this.instrumentation;
    }

    public final ClassNameFilter getClassNameFilter() {
        return this.classNameFilter;
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
                                 byte[] classfileBuffer, Match match) {
            ClassReader cr = new ClassReader(classfileBuffer);
            if (InstrumentationUtils.isInterface(cr)) {
                return null;
            }

            Collection strongMatches = Lists.newArrayList(ClassTransformer.this.pointcuts);
            strongMatches.retainAll(match.getClassMatches().keySet());

            if (strongMatches.isEmpty()) {
                return null;
            }

            if ((this.classLoader != null) && (!InstrumentationUtils
                                                        .isAbleToResolveAgent(this.classLoader, className))) {
                String msg = MessageFormat.format("Not instrumenting {0}: class loader unable to load agent classes",
                                                         new Object[] {className});

                Agent.LOG.log(Level.FINER, msg);
                return null;
            }
            try {
                if (ClassTransformer.canModifyClassStructure(this.classLoader, classBeingRedefined)) {
                    byte[] classfileBufferWithUID = InstrumentationUtils.generateClassBytesWithSerialVersionUID(cr,
                                                                                                                       ClassTransformer.this.classreaderFlags,
                                                                                                                       this.classLoader);

                    cr = new ClassReader(classfileBufferWithUID);
                }

                ClassWriter cw = InstrumentationUtils.getClassWriter(cr, this.classLoader);

                GenericClassAdapter adapter =
                        new GenericClassAdapter(cw, this.classLoader, className, classBeingRedefined, strongMatches,
                                                       context);

                cr.accept(adapter, ClassTransformer.this.classreaderFlags);

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

                ClassTransformer.this.logger.warning(msg);
                if (ClassTransformer.this.logger.isLoggable(Level.FINER)) {
                    msg = MessageFormat.format("ASM error for pointcut(s) : strong {0}", new Object[] {strongMatches});
                    ClassTransformer.this.logger.finer(msg);
                    ClassTransformer.this.logger.log(Level.FINER, "ASM error", t);
                }
                if (Boolean.getBoolean("newrelic.asm.error.stop")) {
                    System.exit(-1);
                }
                return null;
            } catch (ThreadDeath e) {
                throw e;
            } catch (Throwable t) {
                ClassTransformer.this.logger.warning(MessageFormat
                                                             .format("Skipping transformation of class {0} because an"
                                                                             + " error occurred: {1}",
                                                                            new Object[] {className, t.toString()}));

                if (ClassTransformer.this.logger.isLoggable(Level.FINER)) {
                    ClassTransformer.this.logger.log(Level.FINER, "Error transforming class " + className, t);
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
                ClassTransformer.this.instrumentation
                        .redefineClasses(new ClassDefinition[] {new ClassDefinition(this.classLoader
                                                                                            .loadClass(Invoker.getClassNameFromInternalName(className)),
                                                                                           classfileBuffer)});
            } catch (ClassNotFoundException e) {
                String msg = MessageFormat.format("An error occurred redefining class {0}: {1}",
                                                         new Object[] {className, e});
                if (ClassTransformer.this.logger.isLoggable(Level.FINEST)) {
                    ClassTransformer.this.logger.log(Level.FINEST, msg, e);
                } else if (ClassTransformer.this.logger.isLoggable(Level.FINER)) {
                    ClassTransformer.this.logger.finer(msg);
                }
            } catch (UnmodifiableClassException e) {
                String msg = MessageFormat.format("An error occurred redefining class {0}: {1}",
                                                         new Object[] {className, e});
                if (ClassTransformer.this.logger.isLoggable(Level.FINEST)) {
                    ClassTransformer.this.logger.log(Level.FINEST, msg, e);
                } else if (ClassTransformer.this.logger.isLoggable(Level.FINER)) {
                    ClassTransformer.this.logger.finer(msg);
                }
            }
        }

        public ClassLoader getClassLoader() {
            return this.classLoader;
        }
    }
}