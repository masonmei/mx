package com.newrelic.agent.instrumentation.context;

import java.io.File;
import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.Config;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.InstrumentedClass;
import com.newrelic.agent.instrumentation.InstrumentedMethod;
import com.newrelic.agent.instrumentation.PointCut;
import com.newrelic.agent.instrumentation.api.ApiImplementationUpdate;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher;
import com.newrelic.agent.instrumentation.ejb3.EJBAnnotationVisitor;
import com.newrelic.agent.instrumentation.spring.SpringAnnotationVisitor;
import com.newrelic.agent.instrumentation.tracing.TraceClassTransformer;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.agent.instrumentation.weaver.ClassWeaverService;
import com.newrelic.agent.instrumentation.weaver.NewClassMarker;
import com.newrelic.agent.instrumentation.weaver.WeavedMethod;
import com.newrelic.agent.instrumentation.webservices.RestAnnotationVisitor;
import com.newrelic.agent.instrumentation.webservices.WebServiceVisitor;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.servlet.ServletAnnotationVisitor;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.agent.util.asm.PatchedClassWriter;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.deps.com.google.common.collect.ImmutableSet;
import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.org.objectweb.asm.AnnotationVisitor;
import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.ClassWriter;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.JSRInlinerAdapter;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

public class InstrumentationContextManager {
    private static final Set<String> MARKER_INTERFACES_TO_SKIP =
            ImmutableSet.of("org/hibernate/proxy/HibernateProxy", "org/springframework/aop/SpringProxy");

    private static final ContextClassTransformer NO_OP_TRANSFORMER = new ContextClassTransformer() {
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer,
                                InstrumentationContext context, OptimizedClassMatcher.Match match)
                throws IllegalClassFormatException {
            return null;
        }
    };
    private static final Set<String> ANNOTATIONS_TO_REMOVE = ImmutableSet
                                                                     .of(Type.getDescriptor(InstrumentedClass.class),
                                                                                Type.getDescriptor(InstrumentedMethod
                                                                                                           .class),
                                                                                Type.getDescriptor(WeavedMethod.class));
    private final Map<ClassMatchVisitorFactory, ContextClassTransformer> matchVisitors = Maps.newConcurrentMap();
    private final Map<ClassMatchVisitorFactory, ContextClassTransformer> interfaceMatchVisitors =
            Maps.newConcurrentMap();
    private final Instrumentation instrumentation;
    private final ClassWeaverService classWeaverService;
    ClassFileTransformer transformer;
    private ClassChecker classChecker;
    private final ContextClassTransformer FinishClassTransformer = new ContextClassTransformer() {
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer,
                                InstrumentationContext context, OptimizedClassMatcher.Match match)
                throws IllegalClassFormatException {
            try {
                return getFinalTransformation(loader, className, classBeingRedefined, classfileBuffer, context);
            } catch (Throwable ex) {
                Agent.LOG.log(Level.FINE, "Unable to transform " + className, ex);
            }

            return null;
        }

        private byte[] getFinalTransformation(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                              byte[] classfileBuffer, InstrumentationContext context) {
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassWriter writer = new PatchedClassWriter(2, context.getClassResolver(loader));

            ClassVisitor cv = writer;

            if (!context.getWeavedMethods().isEmpty()) {
                cv = new MarkWeaverMethodsVisitor(cv, context);
            }

            cv = addModifiedClassAnnotation(cv, context);
            cv = addModifiedMethodAnnotation(cv, context, loader);

            cv = new ClassVisitor(Agent.ASM_LEVEL, cv) {
                public void visit(int version, int access, String name, String signature, String superName,
                                  String[] interfaces) {
                    if ((version < 49) || (version > 100)) {
                        Agent.LOG.log(Level.FINEST, "Converting {0} from version {1} to {2}", name, version, 49);
                        version = 49;
                    }
                    super.visit(version, access, name, signature, superName, interfaces);
                }

                public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                                 String[] exceptions) {
                    return new JSRInlinerAdapter(super.visitMethod(access, name, desc, signature, exceptions), access,
                                                        name, desc, signature, exceptions);
                }
            };
            cv = skipExistingAnnotations(cv);

            cv = CurrentTransactionRewriter.rewriteCurrentTransactionReferences(cv, reader);

            reader.accept(cv, 4);

            if (classChecker != null) {
                classChecker.check(writer.toByteArray());
            }

            if (Agent.isDebugEnabled()) {
                try {
                    File old = File.createTempFile(className.replace('/', '_'), ".old");
                    Utils.print(context.bytes, new PrintWriter(old));

                    Agent.LOG.debug("Wrote " + old.getAbsolutePath());
                    File newFile = File.createTempFile(className.replace('/', '_'), ".new");

                    Utils.print(writer.toByteArray(), new PrintWriter(newFile));
                    Agent.LOG.debug("Wrote " + newFile.getAbsolutePath());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            addSupportabilityMetrics(reader, className, context);

            Agent.LOG.finer("Final transformation of class " + className);
            return writer.toByteArray();
        }

        private ClassVisitor skipExistingAnnotations(ClassVisitor cv) {
            return new ClassVisitor(Agent.ASM_LEVEL, cv) {
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    if (InstrumentationContextManager.ANNOTATIONS_TO_REMOVE.contains(desc)) {
                        return null;
                    }
                    return super.visitAnnotation(desc, visible);
                }

                public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                                 String[] exceptions) {
                    return new MethodVisitor(Agent.ASM_LEVEL,
                                                    super.visitMethod(access, name, desc, signature, exceptions)) {
                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            if (InstrumentationContextManager.ANNOTATIONS_TO_REMOVE.contains(desc)) {
                                return null;
                            }
                            return super.visitAnnotation(desc, visible);
                        }
                    };
                }
            };
        }

        private void addSupportabilityMetrics(ClassReader reader, String className, InstrumentationContext context) {
            StatsService statsService = ServiceFactory.getStatsService();
            if (statsService != null) {
                for (Method m : context.getTimedMethods()) {
                    TraceDetails traceDetails = context.getTraceInformation().getTraceAnnotations().get(m);
                    if ((traceDetails != null) && (traceDetails.isCustom())) {
                        statsService.doStatsWork(StatsWorks.getRecordMetricWork(MessageFormat
                                                                                        .format("Supportability/Instrumented/{0}/{1}{2}",
                                                                                                       className.replace('/', '.'),
                                                                                                       m.getName(),
                                                                                                       m.getDescriptor()),
                                                                                       1.0F));
                    }
                }
            }
        }
    };

    public InstrumentationContextManager(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
        classWeaverService = new ClassWeaverService(this);

        matchVisitors.put(new TraceMatchVisitor(), NO_OP_TRANSFORMER);
        matchVisitors.put(new GeneratedClassDetector(), NO_OP_TRANSFORMER);

        AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
        if (agentConfig.getValue("instrumentation.web_services.enabled", true)) {
            matchVisitors.put(new WebServiceVisitor(), NO_OP_TRANSFORMER);
        }

        if (agentConfig.getValue("instrumentation.rest_annotations.enabled", true)) {
            RestAnnotationVisitor rest = new RestAnnotationVisitor();
            matchVisitors.put(rest.getClassMatchVisitorFactory(), NO_OP_TRANSFORMER);
            interfaceMatchVisitors.put(rest.getInterfaceMatchVisitorFactory(this), NO_OP_TRANSFORMER);
        }

        if (agentConfig.getValue("instrumentation.spring_annotations.enabled", true)) {
            SpringAnnotationVisitor rest = new SpringAnnotationVisitor();
            matchVisitors.put(rest.getClassMatchVisitorFactory(), NO_OP_TRANSFORMER);
        }

        if (agentConfig.getValue("instrumentation.servlet_annotations.enabled", true)) {
            matchVisitors.put(new ServletAnnotationVisitor(), NO_OP_TRANSFORMER);
        }

        Config instrumentationConfig = agentConfig.getClassTransformerConfig()
                                               .getInstrumentationConfig("com.newrelic.instrumentation.ejb-3.0");

        if (instrumentationConfig.getProperty("enabled", true)) {
            matchVisitors.put(new EJBAnnotationVisitor(), NO_OP_TRANSFORMER);
        }

        matchVisitors.put(ServiceFactory.getJarCollectorService().getSourceVisitor(), NO_OP_TRANSFORMER);
        try {
            ApiImplementationUpdate.setup(this);
        } catch (Exception e) {
            Agent.LOG.log(Level.FINEST, e.toString(), e);
        }
    }

    public static InstrumentationContextManager create(InstrumentationProxy instrumentation,
                                                       final boolean bootstrapClassloaderEnabled) throws Exception {
        final InstrumentationContextManager manager = new InstrumentationContextManager(instrumentation);

        final TraceClassTransformer traceTransformer = new TraceClassTransformer();
        Runnable loadWeavedInstrumentation = manager.classWeaverService.registerInstrumentation();

        final boolean[] initialized = new boolean[] {false};

        ClassLoaderClassTransformer classLoaderTransformer = new ClassLoaderClassTransformer(manager);

        ClassFileTransformer transformer = new ClassFileTransformer() {
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer)
                    throws IllegalClassFormatException {
                if (!className.startsWith("com/newrelic/deps/org/objectweb/asm") && !className
                                                                                             .startsWith
                                                                                                      ("com/newrelic/deps")
                            && !className.startsWith("com/newrelic/agent/tracers/")) {
                    if (!initialized[0] && className.startsWith("com/newrelic/")) {
                        return null;
                    } else {
                        if (loader == null) {
                            if (!bootstrapClassloaderEnabled) {
                                return null;
                            }

                            loader = ClassLoader.getSystemClassLoader();
                        }

                        ClassReader reader = new ClassReader(classfileBuffer);
                        if ((8704 & reader.getAccess()) != 0) {
                            manager.applyInterfaceVisitors(loader, classBeingRedefined, reader);
                            return null;
                        } else if (NewClassMarker.isNewWeaveClass(reader)) {
                            return null;
                        } else if (Utils.isJdkProxy(reader)) {
                            Agent.LOG.finest(MessageFormat
                                                     .format("Instrumentation skipped by \'\'JDK proxy\'\' rule: {0}",
                                                                    new Object[] {className}));
                            return null;
                        } else {
                            InstrumentationContext context =
                                    new InstrumentationContext(classfileBuffer, classBeingRedefined, protectionDomain);
                            context.match(loader, classBeingRedefined, reader, manager.matchVisitors.keySet());
                            if (context.isGenerated()) {
                                if (context.hasSourceAttribute()) {
                                    Agent.LOG.finest(MessageFormat
                                                             .format("Instrumentation skipped by \'\'generated\'\' "
                                                                             + "rule: {0}", className));
                                } else {
                                    Agent.LOG.finest(MessageFormat
                                                             .format("Instrumentation skipped by \'\'no source\'\' "
                                                                             + "rule: {0}", className));
                                }

                                return null;
                            } else if (!context.getMatches().isEmpty() && InstrumentationContextManager
                                                                                  .skipClass(reader)) {
                                Agent.LOG.finest(MessageFormat
                                                         .format("Instrumentation skipped by \'\'class name\'\' rule:"
                                                                         + " {0}", className));
                                return null;
                            } else {

                                for (Map.Entry<ClassMatchVisitorFactory, OptimizedClassMatcher.Match> entry : context.getMatches().entrySet()) {
                                    ContextClassTransformer transformer = manager.matchVisitors.get(entry.getKey());
                                    if (transformer != null
                                                && transformer != InstrumentationContextManager.NO_OP_TRANSFORMER) {
                                        byte[] bytes1 = transformer.transform(loader, className, classBeingRedefined,
                                                                                     protectionDomain, classfileBuffer,
                                                                                     context, entry.getValue());
                                        classfileBuffer = context.processTransformBytes(classfileBuffer, bytes1);
                                    } else {
                                        Agent.LOG.fine("Unable to find a class transformer to process match "
                                                               + entry.getValue());
                                    }
                                }

                                if (context.isTracerMatch()) {
                                    byte[] bytes2 = traceTransformer.transform(loader, className, classBeingRedefined,
                                                                                      protectionDomain, classfileBuffer,
                                                                                      context, null);
                                    classfileBuffer = context.processTransformBytes(classfileBuffer, bytes2);
                                }

                                if (context.isModified()) {
                                    return manager.FinishClassTransformer
                                                   .transform(loader, className, classBeingRedefined, protectionDomain,
                                                                     classfileBuffer, context, null);
                                }

                                return null;

                            }
                        }
                    }
                } else {
                    return null;
                }
            }
        };
        //        ClassFileTransformer transformer = new ClassFileTransformer() {
        //            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
        //                                    ProtectionDomain protectionDomain, byte[] classfileBuffer)
        //                    throws IllegalClassFormatException {
        //                if ((className.startsWith("com/newrelic/deps/org/objectweb/asm")) || (className
        //                                                                                              .startsWith
        //
        // ("com/newrelic/deps"))
        //                            || (className.startsWith("com/newrelic/agent/tracers/"))) {
        //                    return null;
        //                }
        //                if (initialized[0] && (className.startsWith("com/newrelic/"))) {
        //                    return null;
        //                }
        //                if (loader == null) {
        //                    if (!bootstrapClassloaderEnabled) {
        //                        return null;
        //                    }
        //                    loader = ClassLoader.getSystemClassLoader();
        //                }
        //
        //                ClassReader reader = new ClassReader(classfileBuffer);
        //                if ((0x2200 & reader.getAccess()) != 0) {
        //                    manager.applyInterfaceVisitors(loader, classBeingRedefined, reader);
        //                    return null;
        //                }
        //                if (NewClassMarker.isNewWeaveClass(reader)) {
        //                    return null;
        //                }
        //
        //                if (Utils.isJdkProxy(reader)) {
        //                    Agent.LOG.finest(MessageFormat
        //                                             .format("Instrumentation skipped by ''JDK proxy'' rule: {0}",
        // className));
        //                    return null;
        //                }
        //
        //                InstrumentationContext context =
        //                        new InstrumentationContext(classfileBuffer, classBeingRedefined, protectionDomain);
        //
        //                context.match(loader, classBeingRedefined, reader, manager.matchVisitors.keySet());
        //
        //                if (context.isGenerated()) {
        //                    if (context.hasSourceAttribute()) {
        //                        Agent.LOG.finest(MessageFormat.format("Instrumentation skipped by ''generated''
        // rule: {0}",
        //                                                                     className));
        //                    } else {
        //                        Agent.LOG.finest(MessageFormat.format("Instrumentation skipped by ''no source''
        // rule: {0}",
        //                                                                     className));
        //                    }
        //                    return null;
        //                }
        //
        //                if ((!context.getMatches().isEmpty()) && (InstrumentationContextManager.skipClass(reader))) {
        //                    Agent.LOG.finest(MessageFormat
        //                                             .format("Instrumentation skipped by ''class name'' rule: {0}",
        // className));
        //                    return null;
        //                }
        //
        //                for (Map.Entry entry : context.getMatches().entrySet()) {
        //                    ContextClassTransformer transformer = manager.matchVisitors.get(entry.getKey());
        //                    if ((transformer != null) && (transformer != InstrumentationContextManager
        // .NO_OP_TRANSFORMER)) {
        //                        byte[] bytes = transformer.transform(loader, className, classBeingRedefined,
        // protectionDomain,
        //                                                                    classfileBuffer, context,
        //                                                                    (OptimizedClassMatcher.Match) entry
        // .getValue());
        //
        //                        classfileBuffer = context.processTransformBytes(classfileBuffer, bytes);
        //                    } else {
        //                        Agent.LOG.fine("Unable to find a class transformer to process match " + entry
        // .getValue());
        //                    }
        //                }
        //
        //                if (context.isTracerMatch()) {
        //                    byte[] bytes = traceTransformer.transform(loader, className, classBeingRedefined,
        // protectionDomain,
        //                                                                     classfileBuffer, context, null);
        //
        //                    classfileBuffer = context.processTransformBytes(classfileBuffer, bytes);
        //                }
        //
        //                if (context.isModified()) {
        //                    return manager.FinishClassTransformer
        //                                   .transform(loader, className, classBeingRedefined, protectionDomain,
        // classfileBuffer,
        //                                                     context, null);
        //                }
        //
        //                return null;
        //            }
        //        };
        instrumentation.addTransformer(transformer, true);
        manager.transformer = transformer;

        loadWeavedInstrumentation.run();
        classLoaderTransformer.start(instrumentation);

        initialized[0] = true;

        return manager;
    }

    private static boolean skipClass(ClassReader reader) {
        for (String interfaceName : reader.getInterfaces()) {
            if (MARKER_INTERFACES_TO_SKIP.contains(interfaceName)) {
                return true;
            }
        }
        return false;
    }

    public ClassWeaverService getClassWeaverService() {
        return classWeaverService;
    }

    private void applyInterfaceVisitors(ClassLoader loader, Class<?> classBeingRedefined, ClassReader reader) {
        ClassVisitor cv = null;
        for (ClassMatchVisitorFactory factory : interfaceMatchVisitors.keySet()) {
            cv = factory.newClassMatchVisitor(loader, classBeingRedefined, reader, cv, null);
        }
        if (cv != null) {
            reader.accept(cv, 1);
        }
    }

    public void addContextClassTransformer(ClassMatchVisitorFactory matchVisitor, ContextClassTransformer transformer) {
        if (transformer == null) {
            transformer = NO_OP_TRANSFORMER;
        }
        matchVisitors.put(matchVisitor, transformer);
    }

    public void removeMatchVisitor(ClassMatchVisitorFactory visitor) {
        matchVisitors.remove(visitor);
    }

    protected ClassVisitor addModifiedMethodAnnotation(ClassVisitor cv, final InstrumentationContext context,
                                                       final ClassLoader loader) {
        return new ClassVisitor(Agent.ASM_LEVEL, cv) {
            private String className;

            public void visit(int version, int access, String name, String signature, String superName,
                              String[] interfaces) {
                className = name;
                super.visit(version, access, name, signature, superName, interfaces);
            }

            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                             String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                Method method = new Method(name, desc);
                if ((context.isModified(method)) && (loader != null)) {
                    TraceDetails traceDetails = context.getTraceInformation().getTraceAnnotations().get(method);
                    boolean dispatcher = false;
                    if (traceDetails != null) {
                        dispatcher = traceDetails.dispatcher();
                    }

                    AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(InstrumentedMethod.class), true);
                    av.visit("dispatcher", dispatcher);
                    List<String> instrumentationNames = Lists.newArrayList();
                    List<InstrumentationType> instrumentationTypes = Lists.newArrayList();
                    Level logLevel = Level.FINER;

                    if (traceDetails != null) {
                        if (traceDetails.instrumentationSourceNames() != null) {
                            instrumentationNames.addAll(traceDetails.instrumentationSourceNames());
                        }
                        if (traceDetails.instrumentationTypes() != null) {
                            for (InstrumentationType type : traceDetails.instrumentationTypes()) {
                                if (type == InstrumentationType.WeaveInstrumentation) {
                                    instrumentationTypes.add(InstrumentationType.TracedWeaveInstrumentation);
                                } else {
                                    instrumentationTypes.add(type);
                                }
                            }
                        }
                        if (traceDetails.isCustom()) {
                            logLevel = Level.FINE;
                        }
                    }
                    PointCut pointCut = context.getOldStylePointCut(method);
                    if (pointCut != null) {
                        instrumentationNames.add(pointCut.getClass().getName());
                        instrumentationTypes.add(InstrumentationType.Pointcut);
                    }
                    Collection<String> instrumentationPackages = context.getMergeInstrumentationPackages(method);

                    if ((instrumentationPackages != null) && (!instrumentationPackages.isEmpty())) {
                        for (String current : instrumentationPackages) {
                            instrumentationNames.add(current);
                            instrumentationTypes.add(InstrumentationType.WeaveInstrumentation);
                        }
                    }

                    if (instrumentationNames.size() == 0) {
                        instrumentationNames.add("Unknown");
                        Agent.LOG.finest("Unknown instrumentation source for " + className + '.' + method);
                    }
                    if (instrumentationTypes.size() == 0) {
                        instrumentationTypes.add(InstrumentationType.Unknown);
                        Agent.LOG.finest("Unknown instrumentation type for " + className + '.' + method);
                    }

                    AnnotationVisitor visitArrayName = av.visitArray("instrumentationNames");
                    for (String current : instrumentationNames) {
                        visitArrayName.visit("", current);
                    }
                    visitArrayName.visitEnd();

                    AnnotationVisitor visitArrayType = av.visitArray("instrumentationTypes");
                    for (InstrumentationType type : instrumentationTypes) {
                        visitArrayType.visitEnum("", Type.getDescriptor(InstrumentationType.class), type.toString());
                    }
                    visitArrayType.visitEnd();

                    av.visitEnd();

                    if (Agent.LOG.isLoggable(logLevel)) {
                        Agent.LOG.log(logLevel, "Instrumented " + Type.getObjectType(className).getClassName() + '.'
                                                        + method + ", " + instrumentationTypes + ", "
                                                        + instrumentationNames);
                    }

                }

                return mv;
            }
        };
    }

    protected ClassVisitor addModifiedClassAnnotation(ClassVisitor cv, InstrumentationContext context) {
        AnnotationVisitor visitAnnotation = cv.visitAnnotation(Type.getDescriptor(InstrumentedClass.class), true);

        if (context.isUsingLegacyInstrumentation()) {
            visitAnnotation.visit("legacy", Boolean.TRUE);
        }
        if (context.hasModifiedClassStructure()) {
            visitAnnotation.visit("classStructureModified", Boolean.TRUE);
        }
        visitAnnotation.visitEnd();

        return cv;
    }

    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    public void setClassChecker(ClassChecker classChecker) {
        this.classChecker = classChecker;
    }

    private static class MarkWeaverMethodsVisitor extends ClassVisitor {
        private final InstrumentationContext context;

        public MarkWeaverMethodsVisitor(ClassVisitor cv, InstrumentationContext context) {
            super(Agent.ASM_LEVEL, cv);
            this.context = context;
        }

        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

            Collection<String> instrumentationTitles = context.getMergeInstrumentationPackages(new Method(name, desc));
            if ((instrumentationTitles != null) && (!instrumentationTitles.isEmpty())) {
                AnnotationVisitor weavedAnnotation = mv.visitAnnotation(Type.getDescriptor(WeavedMethod.class), true);

                AnnotationVisitor visitArray = weavedAnnotation.visitArray("source");
                for (String title : instrumentationTitles) {
                    visitArray.visit("", title);
                }
                visitArray.visitEnd();
                weavedAnnotation.visitEnd();
            }

            return mv;
        }
    }
}