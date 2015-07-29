//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.context;

import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

import com.newrelic.deps.org.objectweb.asm.AnnotationVisitor;
import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.ClassWriter;
import com.newrelic.deps.org.objectweb.asm.Label;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.AdviceAdapter;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

import com.newrelic.deps.com.google.common.collect.ImmutableSet;
import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.agent.Agent;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.config.ClassTransformerConfig;
import com.newrelic.agent.config.IBMUtils;
import com.newrelic.agent.instrumentation.MethodBuilder;
import com.newrelic.agent.instrumentation.classmatchers.ChildClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.DefaultClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher.Match;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcherBuilder;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.instrumentation.tracing.BridgeUtils;
import com.newrelic.agent.service.ServiceFactory;

class ClassLoaderClassTransformer implements ContextClassTransformer {
    public static final String NEWRELIC_CLASS_PREFIX = "com.newrelic.agent.";
    private static final String NEWRELIC_API_CLASS_PREFIX = "com.newrelic.api.agent.";
    private static final Type CLASSLOADER_TYPE = Type.getType(ClassLoader.class);
    private static final Method LOAD_CLASS_METHOD =
            new Method("loadClass", Type.getType(Class.class), new Type[] {Type.getType(String.class)});
    private static final Method LOAD_CLASS_RESOLVE_METHOD;
    private static final Set<Method> METHODS;
    private static final Method CHECK_PACKAGE_ACCESS_METHOD;

    static {
        LOAD_CLASS_RESOLVE_METHOD = new Method("loadClass", Type.getType(Class.class),
                                                      new Type[] {Type.getType(String.class),
                                                                         Type.getType(Boolean.TYPE)});
        METHODS = ImmutableSet.of(LOAD_CLASS_METHOD, LOAD_CLASS_RESOLVE_METHOD);
        CHECK_PACKAGE_ACCESS_METHOD = new Method("checkPackageAccess", Type.VOID_TYPE,
                                                        new Type[] {Type.getType(Class.class),
                                                                           Type.getType(ProtectionDomain.class)});
    }

    private final ClassMatchVisitorFactory matcher;
    private final Set<String> classloadersToSkip;

    public ClassLoaderClassTransformer(InstrumentationContextManager manager) {
        OptimizedClassMatcherBuilder matcherBuilder = OptimizedClassMatcherBuilder.newBuilder();
        matcherBuilder
                .addClassMethodMatcher(new ClassAndMethodMatcher[] {new DefaultClassAndMethodMatcher(new ChildClassMatcher(CLASSLOADER_TYPE
                                                                                                                                   .getInternalName(),
                                                                                                                                  false),
                                                                                                            OrMethodMatcher
                                                                                                                    .getMethodMatcher(new MethodMatcher[] {new ExactMethodMatcher(LOAD_CLASS_METHOD
                                                                                                                                                                                          .getName(),
                                                                                                                                                                                         LOAD_CLASS_METHOD
                                                                                                                                                                                                 .getDescriptor()),
                                                                                                                                                                  new ExactMethodMatcher(LOAD_CLASS_RESOLVE_METHOD
                                                                                                                                                                                                 .getName(),
                                                                                                                                                                                                LOAD_CLASS_RESOLVE_METHOD
                                                                                                                                                                                                        .getDescriptor())}))});
        this.matcher = matcherBuilder.build();
        manager.addContextClassTransformer(this.matcher, this);
        String agentClassloaderName = Type.getType(Agent.getClassLoader().getClass()).getInternalName();
        this.classloadersToSkip = ImmutableSet.of("com/ibm/oti/vm/BootstrapClassLoader", "sun/reflect/misc/MethodUtil",
                                                         agentClassloaderName);
    }

    public static boolean isClassLoaderModified(Class<ClassLoader> classLoaderClass) {
        return classLoaderClass.getAnnotation(ModifiedClassloader.class) != null;
    }

    void start(Instrumentation instrumentation) {
        ArrayList toRetransform = Lists.newArrayList();
        Class[] e = instrumentation.getAllLoadedClasses();
        int classloader = e.length;

        for (int e1 = 0; e1 < classloader; ++e1) {
            Class clazz = e[e1];
            if (ClassLoader.class.isAssignableFrom(clazz) && !clazz.getName().startsWith("java.") && !clazz.getName()
                                                                                                              .startsWith("sun.")
                        && !this.classloadersToSkip.contains(Type.getType(clazz).getInternalName())) {
                toRetransform.add(clazz);
            }
        }

        if (!toRetransform.isEmpty()) {
            Agent.LOG.log(Level.FINER, "Retransforming {0}", new Object[] {toRetransform.toString()});
            Iterator var9 = toRetransform.iterator();

            while (var9.hasNext()) {
                Class var10 = (Class) var9.next();

                try {
                    instrumentation.retransformClasses(new Class[] {var10});
                } catch (UnmodifiableClassException var8) {
                    Agent.LOG.log(Level.FINE, "classloader transformer: Error retransforming {0}",
                                         new Object[] {var10.getName()});
                }
            }
        }

        if (ServiceFactory.getConfigService().getDefaultAgentConfig().getIbmWorkaroundEnabled()) {
            Agent.LOG.log(Level.FINE,
                                 "classloader transformer: skipping redefine of {0}. IBM SR {1}. java.runtime.version"
                                         + " {2}",
                                 new Object[] {ClassLoader.class.getName(), Integer.valueOf(IBMUtils.getIbmSRNumber()),
                                                      System.getProperty("java.runtime.version")});
        } else {
            try {
                Agent.LOG.log(Level.FINER, "classloader transformer: Attempting to redefine {0}",
                                     new Object[] {ClassLoader.class});
                InstrumentationProxy.forceRedefinition(instrumentation, new Class[] {ClassLoader.class});
            } catch (Exception var7) {
                Agent.LOG.log(Level.FINEST, var7, "classloader transformer: Error redefining {0}",
                                     new Object[] {ClassLoader.class.getName()});
            }

            Agent.LOG.log(Level.FINE, "classloader transformer: {0} isClassLoaderModified = {1}",
                                 new Object[] {ClassLoader.class,
                                                      Boolean.valueOf(isClassLoaderModified(ClassLoader.class))});
        }

    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer, InstrumentationContext context,
                            Match match) throws IllegalClassFormatException {
        if (this.classloadersToSkip.contains(className)) {
            Agent.LOG.log(Level.FINEST, "classloader transformer: classloadersToSkip contains {0}",
                                 new Object[] {className});
            return null;
        } else {
            try {
                if (match.isClassAndMethodMatch()) {
                    return this.transformBytes(className, classfileBuffer);
                }
            } catch (Exception var9) {
                Agent.LOG.log(Level.FINER, var9, "classloader transformer: Error transforming {0}",
                                     new Object[] {className});
            }

            return null;
        }
    }

    byte[] transformBytes(String className, byte[] classfileBuffer) {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(1);
        reader.accept(new ClassLoaderClassTransformer.ClassLoaderClassVisitor(writer), 8);
        Agent.LOG.log(Level.FINER, "class transformer: Patching {0}", new Object[] {className});
        return writer.toByteArray();
    }

    private class ClassLoaderClassVisitor extends ClassVisitor {
        Set<Method> methods;

        public ClassLoaderClassVisitor(ClassVisitor cv) {
            super(Agent.ASM_LEVEL, cv);
            this.methods = ClassLoaderClassTransformer.METHODS;
        }

        public void visit(int version, int access, String name, String signature, String superName,
                          String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            AnnotationVisitor visitAnnotation =
                    this.visitAnnotation(Type.getDescriptor(ModifiedClassloader.class), true);
            visitAnnotation.visitEnd();
            if (ClassLoaderClassTransformer.CLASSLOADER_TYPE.getInternalName().equals(name)) {
                this.methods = ImmutableSet.of(ClassLoaderClassTransformer.LOAD_CLASS_METHOD);
                Agent.LOG.log(Level.FINEST, "class transformer: Updated method matcher for {0}", new Object[] {name});
            }

        }

        public MethodVisitor visitMethod(final int access, final String name, final String desc, String signature,
                                         String[] exceptions) {

            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            Method method = new Method(name, desc);
            if (this.methods.contains(method)) {
                mv = new AdviceAdapter(Agent.ASM_LEVEL, (MethodVisitor) mv, access, name, desc) {
                    Label startFinallyLabel;

                    protected void onMethodEnter() {
                        this.startFinallyLabel = this.newLabel();
                        this.loadArg(0);
                        this.mv.visitLdcInsn("com.newrelic.api.agent.");
                        this.mv.visitMethodInsn(182, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false);
                        this.mv.visitJumpInsn(153, this.startFinallyLabel);
                        this.loadNewRelicClass();
                        this.visitLabel(this.startFinallyLabel);
                    }

                    public void visitMaxs(int maxStack, int maxLocals) {
                        Label endFinallyLabel = new Label();
                        super.visitTryCatchBlock(this.startFinallyLabel, endFinallyLabel, endFinallyLabel,
                                                        Type.getType(ClassNotFoundException.class).getInternalName());
                        super.visitLabel(endFinallyLabel);
                        this.onMethodExit(191);
                        super.visitMaxs(maxStack, maxLocals);
                    }

                    protected void onMethodExit(int opcode) {
                        if (opcode == 191) {
                            this.loadArg(0);
                            this.mv.visitLdcInsn("com.newrelic.agent.");
                            this.mv.visitMethodInsn(182, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z",
                                                           false);
                            Label skip = this.newLabel();
                            this.mv.visitJumpInsn(153, skip);
                            this.loadNewRelicClass();
                            this.mv.visitLabel(skip);
                            this.mv.visitInsn(191);
                        }

                    }

                    private void loadNewRelicClass() {
                        MethodBuilder methodBuilder = new MethodBuilder(this, access);
                        methodBuilder.loadInvocationHandlerFromProxy();
                        this.mv.visitLdcInsn("CLASSLOADER");
                        this.mv.visitInsn(1);
                        this.mv.visitInsn(1);
                        methodBuilder.invokeInvocationHandlerInterface(false);
                        this.checkCast(Type.getType(ClassLoader.class));
                        Label isNRClassLoader = this.newLabel();
                        this.dup();
                        this.loadThis();
                        this.ifCmp(ClassLoaderClassTransformer.CLASSLOADER_TYPE, 153, isNRClassLoader);
                        this.loadArg(0);
                        this.mv.visitMethodInsn(182, "java/lang/ClassLoader",
                                                       ClassLoaderClassTransformer.LOAD_CLASS_METHOD.getName(),
                                                       ClassLoaderClassTransformer.LOAD_CLASS_METHOD.getDescriptor(),
                                                       false);
                        this.mv.visitInsn(176);
                        this.mv.visitLabel(isNRClassLoader);
                        this.pop();
                    }
                };
            } else if (ClassLoaderClassTransformer.CHECK_PACKAGE_ACCESS_METHOD.equals(method)
                               && System.getSecurityManager() != null) {
                ClassTransformerConfig config =
                        ServiceFactory.getConfigService().getDefaultAgentConfig().getClassTransformerConfig();
                if (config.isGrantPackageAccess()) {
                    mv = new AdviceAdapter(Agent.ASM_LEVEL, (MethodVisitor) mv, access, name, desc) {
                        protected void onMethodEnter() {
                            this.getStatic(BridgeUtils.AGENT_BRIDGE_TYPE, "instrumentation",
                                                  BridgeUtils.INSTRUMENTATION_TYPE);
                            this.loadArg(0);
                            this.invokeInterface(BridgeUtils.INSTRUMENTATION_TYPE,
                                                        new Method("isWeaveClass", Type.BOOLEAN_TYPE,
                                                                          new Type[] {Type.getType(Class.class)}));
                            Label skip = this.newLabel();
                            this.ifZCmp(153, skip);
                            this.visitInsn(177);
                            this.visitLabel(skip);
                            super.onMethodEnter();
                        }
                    };
                }
            }

            return (MethodVisitor) mv;
        }
    }
}
