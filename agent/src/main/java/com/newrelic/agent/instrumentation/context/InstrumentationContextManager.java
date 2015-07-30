//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.context;

import java.io.File;
import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
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
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher.Match;
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
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.JSRInlinerAdapter;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

public class InstrumentationContextManager {
    private static final Set<String> MARKER_INTERFACES_TO_SKIP = ImmutableSet.of("org/hibernate/proxy/HibernateProxy", "org/springframework/aop/SpringProxy");
    private static final ContextClassTransformer NO_OP_TRANSFORMER = new ContextClassTransformer() {
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer, InstrumentationContext context, Match match) throws IllegalClassFormatException {
            return null;
        }
    };
    private final Map<ClassMatchVisitorFactory, ContextClassTransformer> matchVisitors = Maps.newConcurrentMap();
    private final Map<ClassMatchVisitorFactory, ContextClassTransformer> interfaceMatchVisitors = Maps.newConcurrentMap();
    private final Instrumentation instrumentation;
    private ClassChecker classChecker;
    private final ClassWeaverService classWeaverService;
    ClassFileTransformer transformer;
    private static final Set<String> ANNOTATIONS_TO_REMOVE = ImmutableSet.of(Type.getDescriptor(InstrumentedClass.class), Type.getDescriptor(InstrumentedMethod.class), Type.getDescriptor(WeavedMethod.class));
    private final ContextClassTransformer FinishClassTransformer = new ContextClassTransformer() {
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer, InstrumentationContext context, Match match) throws IllegalClassFormatException {
            try {
                return this.getFinalTransformation(loader, className, classBeingRedefined, classfileBuffer, context);
            } catch (Throwable var9) {
                Agent.LOG.log(Level.FINE, "Unable to transform " + className, var9);
                return null;
            }
        }

        private byte[] getFinalTransformation(ClassLoader loader, String className, Class<?> classBeingRedefined, byte[] classfileBuffer, InstrumentationContext context) {
            ClassReader reader = new ClassReader(classfileBuffer);
            PatchedClassWriter writer = new PatchedClassWriter(2, context.getClassResolver(loader));
            Object cv = writer;
            if(!context.getWeavedMethods().isEmpty()) {
                cv = new InstrumentationContextManager.MarkWeaverMethodsVisitor(writer, context);
            }

            ClassVisitor cv1 = addModifiedClassAnnotation((ClassVisitor) cv, context);
            cv1 = InstrumentationContextManager.this.addModifiedMethodAnnotation(cv1, context, loader);
            cv1 = new ClassVisitor(Agent.ASM_LEVEL, cv1) {
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    if(version < 49 || version > 100) {
                        Agent.LOG.log(Level.FINEST, "Converting {0} from version {1} to {2}", name, version, 49);
                        version = 49;
                    }

                    super.visit(version, access, name, signature, superName, interfaces);
                }

                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    return new JSRInlinerAdapter(super.visitMethod(access, name, desc, signature, exceptions), access, name, desc, signature, exceptions);
                }
            };
            cv1 = this.skipExistingAnnotations(cv1);
            cv1 = CurrentTransactionRewriter.rewriteCurrentTransactionReferences(cv1, reader);
            reader.accept(cv1, 4);
            if(InstrumentationContextManager.this.classChecker != null) {
                InstrumentationContextManager.this.classChecker.check(writer.toByteArray());
            }

            if(Agent.isDebugEnabled()) {
                try {
                    File ex = File.createTempFile(className.replace('/', '_'), ".old");
                    Utils.print(context.bytes, new PrintWriter(ex));
                    Agent.LOG.debug("Wrote " + ex.getAbsolutePath());
                    File newFile = File.createTempFile(className.replace('/', '_'), ".new");
                    Utils.print(writer.toByteArray(), new PrintWriter(newFile));
                    Agent.LOG.debug("Wrote " + newFile.getAbsolutePath());
                } catch (Exception var11) {
                    var11.printStackTrace();
                }
            }

            this.addSupportabilityMetrics(reader, className, context);
            Agent.LOG.finer("Final transformation of class " + className);
            return writer.toByteArray();
        }

        private ClassVisitor skipExistingAnnotations(final ClassVisitor cv) {
            return new ClassVisitor(327680, cv) {
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    return InstrumentationContextManager.ANNOTATIONS_TO_REMOVE.contains(desc)?null:super.visitAnnotation(desc, visible);
                }

                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    return new MethodVisitor(327680, super.visitMethod(access, name, desc, signature, exceptions)) {
                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            return InstrumentationContextManager.ANNOTATIONS_TO_REMOVE.contains(desc)?null:super.visitAnnotation(desc, visible);
                        }
                    };
                }
            };
        }

        private void addSupportabilityMetrics(ClassReader reader, String className, InstrumentationContext context) {
            StatsService statsService = ServiceFactory.getStatsService();
            if(statsService != null) {
                Iterator i$ = context.getTimedMethods().iterator();

                while(i$.hasNext()) {
                    Method m = (Method)i$.next();
                    TraceDetails traceDetails = context.getTraceInformation().getTraceAnnotations().get(m);
                    if(traceDetails != null && traceDetails.isCustom()) {
                        statsService.doStatsWork(StatsWorks.getRecordMetricWork(MessageFormat.format("Supportability"
                                                                                                             +
                                                                                                             "/Instrumented/{0}/{1}{2}",
                                                                                                            className
                                                                                                                    .replace('/',
                                                                                                                                    '.'),
                                                                                                            m.getName(),
                                                                                                            m.getDescriptor()), 1.0F));
                    }
                }
            }

        }
    };

    public InstrumentationContextManager(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
        this.classWeaverService = new ClassWeaverService(this);
        this.matchVisitors.put(new TraceMatchVisitor(), NO_OP_TRANSFORMER);
        this.matchVisitors.put(new GeneratedClassDetector(), NO_OP_TRANSFORMER);
        AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
        if(agentConfig.getValue("instrumentation.web_services.enabled", true)) {
            this.matchVisitors.put(new WebServiceVisitor(), NO_OP_TRANSFORMER);
        }

        if(agentConfig.getValue("instrumentation.rest_annotations.enabled", true)) {
            RestAnnotationVisitor instrumentationConfig = new RestAnnotationVisitor();
            this.matchVisitors.put(instrumentationConfig.getClassMatchVisitorFactory(), NO_OP_TRANSFORMER);
            this.interfaceMatchVisitors.put(instrumentationConfig.getInterfaceMatchVisitorFactory(this),
                                                   NO_OP_TRANSFORMER);
        }

        if(agentConfig.getValue("instrumentation.spring_annotations.enabled", true)) {
            SpringAnnotationVisitor instrumentationConfig1 = new SpringAnnotationVisitor();
            this.matchVisitors.put(instrumentationConfig1.getClassMatchVisitorFactory(), NO_OP_TRANSFORMER);
        }

        if(agentConfig.getValue("instrumentation.servlet_annotations.enabled", true)) {
            this.matchVisitors.put(new ServletAnnotationVisitor(), NO_OP_TRANSFORMER);
        }

        Config instrumentationConfig2 = agentConfig.getClassTransformerConfig().getInstrumentationConfig("com.newrelic.instrumentation.ejb-3.0");
        if(instrumentationConfig2.getProperty("enabled", true)) {
            this.matchVisitors.put(new EJBAnnotationVisitor(), NO_OP_TRANSFORMER);
        }

        this.matchVisitors.put(ServiceFactory.getJarCollectorService().getSourceVisitor(), NO_OP_TRANSFORMER);

        try {
            ApiImplementationUpdate.setup(this);
        } catch (Exception var5) {
            Agent.LOG.log(Level.FINEST, var5.toString(), var5);
        }

    }

    public ClassWeaverService getClassWeaverService() {
        return this.classWeaverService;
    }

    public static InstrumentationContextManager create(InstrumentationProxy instrumentation, final boolean bootstrapClassloaderEnabled) throws Exception {
        final InstrumentationContextManager manager = new InstrumentationContextManager(instrumentation);
        final TraceClassTransformer traceTransformer = new TraceClassTransformer();
        Runnable loadWeavedInstrumentation = manager.classWeaverService.registerInstrumentation();
        final boolean[] initialized = new boolean[]{false};
        ClassLoaderClassTransformer classLoaderTransformer = new ClassLoaderClassTransformer(manager);
        ClassFileTransformer transformer = new ClassFileTransformer() {
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                if(!className.startsWith("com/newrelic/deps/org/objectweb/asm") && !className.startsWith("com/newrelic/deps") && !className.startsWith("com/newrelic/agent/tracers/")) {
                    if(!initialized[0] && className.startsWith("com/newrelic/")) {
                        return null;
                    } else {
                        if(loader == null) {
                            if(!bootstrapClassloaderEnabled) {
                                return null;
                            }

                            loader = ClassLoader.getSystemClassLoader();
                        }

                        ClassReader reader = new ClassReader(classfileBuffer);
                        if((8704 & reader.getAccess()) != 0) {
                            manager.applyInterfaceVisitors(loader, classBeingRedefined, reader);
                            return null;
                        } else if(NewClassMarker.isNewWeaveClass(reader)) {
                            return null;
                        } else if(Utils.isJdkProxy(reader)) {
                            Agent.LOG.finest(MessageFormat.format("Instrumentation skipped by \'\'JDK proxy\'\' rule: {0}",
                                                                         className));
                            return null;
                        } else {
                            InstrumentationContext context = new InstrumentationContext(classfileBuffer, classBeingRedefined, protectionDomain);
                            context.match(loader, classBeingRedefined, reader, manager.matchVisitors.keySet());
                            if(context.isGenerated()) {
                                if(context.hasSourceAttribute()) {
                                    Agent.LOG.finest(MessageFormat.format("Instrumentation skipped by \'\'generated\'\' rule: {0}",
                                                                                 className));
                                } else {
                                    Agent.LOG.finest(MessageFormat.format("Instrumentation skipped by \'\'no source\'\' rule: {0}",
                                                                                 className));
                                }

                                return null;
                            } else if(!context.getMatches().isEmpty() && InstrumentationContextManager.skipClass(reader)) {
                                Agent.LOG.finest(MessageFormat.format("Instrumentation skipped by \'\'class name\'\' rule: {0}",
                                                                             className));
                                return null;
                            } else {
                                Iterator bytes = context.getMatches().entrySet().iterator();

                                while(true) {
                                    while(bytes.hasNext()) {
                                        Entry entry = (Entry)bytes.next();
                                        ContextClassTransformer transformer = manager.matchVisitors.get(entry.getKey());
                                        if(transformer != null && transformer != InstrumentationContextManager.NO_OP_TRANSFORMER) {
                                            byte[] bytes1 = transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer, context, (Match)entry.getValue());
                                            classfileBuffer = context.processTransformBytes(classfileBuffer, bytes1);
                                        } else {
                                            Agent.LOG.fine("Unable to find a class transformer to process match " + entry.getValue());
                                        }
                                    }

                                    if(context.isTracerMatch()) {
                                        byte[] bytes2 = traceTransformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer, context,
                                                                                          null);
                                        classfileBuffer = context.processTransformBytes(classfileBuffer, bytes2);
                                    }

                                    if(context.isModified()) {
                                        return manager.FinishClassTransformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer, context,
                                                                                               null);
                                    }

                                    return null;
                                }
                            }
                        }
                    }
                } else {
                    return null;
                }
            }
        };
        instrumentation.addTransformer(transformer, true);
        manager.transformer = transformer;
        loadWeavedInstrumentation.run();
        classLoaderTransformer.start(instrumentation);
        initialized[0] = true;
        return manager;
    }

    private void applyInterfaceVisitors(ClassLoader loader, Class<?> classBeingRedefined, ClassReader reader) {
        ClassVisitor cv = null;

        ClassMatchVisitorFactory factory;
        for(Iterator i$ = this.interfaceMatchVisitors.keySet().iterator(); i$.hasNext(); cv = factory.newClassMatchVisitor(loader, classBeingRedefined, reader, cv,
                                                                                                                                  null)) {
            factory = (ClassMatchVisitorFactory)i$.next();
        }

        if(cv != null) {
            reader.accept(cv, 1);
        }

    }

    private static boolean skipClass(ClassReader reader) {
        String[] arr$ = reader.getInterfaces();
        int len$ = arr$.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            String interfaceName = arr$[i$];
            if(MARKER_INTERFACES_TO_SKIP.contains(interfaceName)) {
                return true;
            }
        }

        return false;
    }

    public void addContextClassTransformer(ClassMatchVisitorFactory matchVisitor, ContextClassTransformer transformer) {
        if(transformer == null) {
            transformer = NO_OP_TRANSFORMER;
        }

        this.matchVisitors.put(matchVisitor, transformer);
    }

    public void removeMatchVisitor(ClassMatchVisitorFactory visitor) {
        this.matchVisitors.remove(visitor);
    }

    protected ClassVisitor addModifiedMethodAnnotation(final ClassVisitor cv, final InstrumentationContext context, final ClassLoader loader) {
        return new ClassVisitor(327680, cv) {
            private String className;

            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.className = name;
                super.visit(version, access, name, signature, superName, interfaces);
            }

            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                Method method = new Method(name, desc);
                if(context.isModified(method) && loader != null) {
                    TraceDetails traceDetails = context.getTraceInformation().getTraceAnnotations().get(method);
                    boolean dispatcher = false;
                    if(traceDetails != null) {
                        dispatcher = traceDetails.dispatcher();
                    }

                    AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(InstrumentedMethod.class), true);
                    av.visit("dispatcher", dispatcher);
                    ArrayList instrumentationNames = Lists.newArrayList();
                    ArrayList instrumentationTypes = Lists.newArrayList();
                    Level logLevel = Level.FINER;
                    if(traceDetails != null) {
                        if(traceDetails.instrumentationSourceNames() != null) {
                            instrumentationNames.addAll(traceDetails.instrumentationSourceNames());
                        }

                        if(traceDetails.instrumentationTypes() != null) {
                            Iterator pointCut = traceDetails.instrumentationTypes().iterator();

                            while(pointCut.hasNext()) {
                                InstrumentationType instrumentationPackages = (InstrumentationType)pointCut.next();
                                if(instrumentationPackages == InstrumentationType.WeaveInstrumentation) {
                                    instrumentationTypes.add(InstrumentationType.TracedWeaveInstrumentation);
                                } else {
                                    instrumentationTypes.add(instrumentationPackages);
                                }
                            }
                        }

                        if(traceDetails.isCustom()) {
                            logLevel = Level.FINE;
                        }
                    }

                    PointCut pointCut1 = context.getOldStylePointCut(method);
                    if(pointCut1 != null) {
                        instrumentationNames.add(pointCut1.getClass().getName());
                        instrumentationTypes.add(InstrumentationType.Pointcut);
                    }

                    Collection instrumentationPackages1 = context.getMergeInstrumentationPackages(method);
                    if(instrumentationPackages1 != null && !instrumentationPackages1.isEmpty()) {
                        Iterator visitArrayName = instrumentationPackages1.iterator();

                        while(visitArrayName.hasNext()) {
                            String visitArrayType = (String)visitArrayName.next();
                            instrumentationNames.add(visitArrayType);
                            instrumentationTypes.add(InstrumentationType.WeaveInstrumentation);
                        }
                    }

                    if(instrumentationNames.size() == 0) {
                        instrumentationNames.add("Unknown");
                        Agent.LOG.finest("Unknown instrumentation source for " + this.className + '.' + method);
                    }

                    if(instrumentationTypes.size() == 0) {
                        instrumentationTypes.add(InstrumentationType.Unknown);
                        Agent.LOG.finest("Unknown instrumentation type for " + this.className + '.' + method);
                    }

                    AnnotationVisitor visitArrayName1 = av.visitArray("instrumentationNames");
                    Iterator visitArrayType1 = instrumentationNames.iterator();

                    while(visitArrayType1.hasNext()) {
                        String i$ = (String)visitArrayType1.next();
                        visitArrayName1.visit("", i$);
                    }

                    visitArrayName1.visitEnd();
                    AnnotationVisitor visitArrayType2 = av.visitArray("instrumentationTypes");
                    Iterator i$1 = instrumentationTypes.iterator();

                    while(i$1.hasNext()) {
                        InstrumentationType type = (InstrumentationType)i$1.next();
                        visitArrayType2.visitEnum("", Type.getDescriptor(InstrumentationType.class), type.toString());
                    }

                    visitArrayType2.visitEnd();
                    av.visitEnd();
                    if(Agent.LOG.isLoggable(logLevel)) {
                        Agent.LOG.log(logLevel, "Instrumented " + Type.getObjectType(this.className).getClassName() + '.' + method + ", " + instrumentationTypes + ", " + instrumentationNames);
                    }
                }

                return mv;
            }
        };
    }

    protected ClassVisitor addModifiedClassAnnotation(ClassVisitor cv, InstrumentationContext context) {
        AnnotationVisitor visitAnnotation = cv.visitAnnotation(Type.getDescriptor(InstrumentedClass.class), true);
        if(context.isUsingLegacyInstrumentation()) {
            visitAnnotation.visit("legacy", Boolean.TRUE);
        }

        if(context.hasModifiedClassStructure()) {
            visitAnnotation.visit("classStructureModified", Boolean.TRUE);
        }

        visitAnnotation.visitEnd();
        return cv;
    }

    public Instrumentation getInstrumentation() {
        return this.instrumentation;
    }

    public void setClassChecker(ClassChecker classChecker) {
        this.classChecker = classChecker;
    }

    private static class MarkWeaverMethodsVisitor extends ClassVisitor {
        private final InstrumentationContext context;

        public MarkWeaverMethodsVisitor(ClassVisitor cv, InstrumentationContext context) {
            super(327680, cv);
            this.context = context;
        }

        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            Collection instrumentationTitles = this.context.getMergeInstrumentationPackages(new Method(name, desc));
            if(instrumentationTitles != null && !instrumentationTitles.isEmpty()) {
                AnnotationVisitor weavedAnnotation = mv.visitAnnotation(Type.getDescriptor(WeavedMethod.class), true);
                AnnotationVisitor visitArray = weavedAnnotation.visitArray("source");
                Iterator i$ = instrumentationTitles.iterator();

                while(i$.hasNext()) {
                    String title = (String)i$.next();
                    visitArray.visit("", title);
                }

                visitArray.visitEnd();
                weavedAnnotation.visitEnd();
            }

            return mv;
        }
    }
}
