//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.AdviceAdapter;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

public class GenericClassAdapter extends ClassVisitor {
    private static final int MAX_VERSION = 51;
    private static final String CLINIT_METHOD_NAME = "<clinit>";
    private static final String NO_ARG_VOID_DESC = "()V";
    private static final String INIT_CLASS_METHOD_NAME = "__nr__initClass";
    protected final String className;
    final Class<?> classBeingRedefined;
    private final ClassLoader classLoader;
    private final List<AbstractTracingMethodAdapter> instrumentedMethods =
            new ArrayList<AbstractTracingMethodAdapter>();
    private final Collection<PointCut> matches;
    private final InstrumentationContext context;
    private int version;
    private boolean processedClassInitMethod;

    public GenericClassAdapter(ClassVisitor cv, ClassLoader classLoader, String className, Class<?> classBeingRedefined,
                               Collection<PointCut> strongMatches, InstrumentationContext context) {
        super(Agent.ASM_LEVEL, cv);
        this.context = context;
        this.matches = strongMatches;
        this.classLoader = classLoader;
        this.className = className;
        this.classBeingRedefined = classBeingRedefined;
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        boolean isInterface = (access & 512) != 0;
        if (isInterface) {
            throw new StopProcessingException(name + " is an interface");
        } else {
            super.visit(version, access, name, signature, superName, interfaces);
            this.version = version;
        }
    }

    boolean canModifyClassStructure() {
        return ClassTransformer.canModifyClassStructure(this.classLoader, this.classBeingRedefined);
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        Object mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (this.canModifyClassStructure() && CLINIT_METHOD_NAME.equals(name)) {
            GenericClassAdapter.InitMethodAdapter mv1 =
                    new GenericClassAdapter.InitMethodAdapter((MethodVisitor) mv, access, name, desc);
            this.processedClassInitMethod = true;
            return mv1;
        } else if ((access & 1024) != 0) {
            return (MethodVisitor) mv;
        } else {
            PointCut pointCut = this.getMatch(access, name, desc);
            if (pointCut == null) {
                return (MethodVisitor) mv;
            } else {
                Method method = new Method(name, desc);
                this.context.addTimedMethods(method);
                if (this.canModifyClassStructure()) {
                    this.context.addOldInvokerStyleInstrumentationMethod(method, pointCut);
                    mv = new InvocationHandlerTracingMethodAdapter(this, (MethodVisitor) mv, access, method);
                } else {
                    PointCutInvocationHandler pointCutInvocationHandler = pointCut.getPointCutInvocationHandler();
                    int id = ServiceFactory.getTracerService().getInvocationHandlerId(pointCutInvocationHandler);
                    if (id == -1) {
                        Agent.LOG.log(Level.FINE, "Unable to find invocation handler for method: {0} in class: {1}. "
                                                          + "Skipping instrumentation.", name, this.className);
                    } else {
                        this.context.addOldReflectionStyleInstrumentationMethod(method, pointCut);
                        mv = new ReflectionStyleClassMethodAdapter(this, (MethodVisitor) mv, access, method, id);
                    }
                }

                return (MethodVisitor) mv;
            }
        }
    }

    private PointCut getMatch(int access, String name, String desc) {
        Iterator i$ = this.matches.iterator();

        PointCut pc;
        do {
            if (!i$.hasNext()) {
                return null;
            }

            pc = (PointCut) i$.next();
        } while (!pc.getMethodMatcher().matches(-1, name, desc, MethodMatcher.UNSPECIFIED_ANNOTATIONS));

        return pc;
    }

    public void visitEnd() {
        super.visitEnd();
        if (this.canModifyClassStructure() && (this.processedClassInitMethod || this.instrumentedMethods.size() > 0)
                    || this.mustAddNRClassInit()) {
            this.createNRClassInitMethod();
        }

        if (this.instrumentedMethods.size() > 0) {
            if (this.canModifyClassStructure() || this.mustAddField()) {
                this.createInvocationHandlerField();
            }

            if ((this.canModifyClassStructure() || this.mustAddNRClassInit()) && !this.processedClassInitMethod) {
                this.createClassInitMethod();
            }
        }

    }

    private boolean mustAddNRClassInit() {
        if (this.classBeingRedefined == null) {
            return false;
        } else {
            try {
                this.classBeingRedefined.getDeclaredMethod(INIT_CLASS_METHOD_NAME, new Class[0]);
                return true;
            } catch (Exception var2) {
                return false;
            }
        }
    }

    private boolean mustAddField() {
        if (this.classBeingRedefined == null) {
            return false;
        } else {
            try {
                this.classBeingRedefined.getDeclaredField("__nr__InvocationHandlers");
                return true;
            } catch (Exception var2) {
                return false;
            }
        }
    }

    private void createClassInitMethod() {
        MethodVisitor mv = this.cv.visitMethod(8, CLINIT_METHOD_NAME, NO_ARG_VOID_DESC, null, null);
        GenericClassAdapter.InitMethodAdapter mv1 =
                new GenericClassAdapter.InitMethodAdapter(mv, 8, CLINIT_METHOD_NAME, NO_ARG_VOID_DESC);
        mv1.visitCode();
        mv1.visitInsn(177);
        mv1.visitMaxs(0, 0);
        mv1.visitEnd();
    }

    private void createNRClassInitMethod() {
        MethodVisitor mv = this.cv.visitMethod(8, INIT_CLASS_METHOD_NAME, NO_ARG_VOID_DESC, null, null);
        GenericClassAdapter.InitMethod mv1 =
                new GenericClassAdapter.InitMethod(mv, 8, INIT_CLASS_METHOD_NAME, NO_ARG_VOID_DESC);
        mv1.visitCode();
        mv1.visitInsn(177);
        mv1.visitMaxs(0, 0);
        mv1.visitEnd();
    }

    private void createInvocationHandlerField() {
        this.cv.visitField(10, "__nr__InvocationHandlers", MethodBuilder.INVOCATION_HANDLER_ARRAY_TYPE.getDescriptor(),
                                  (String) null, (Object) null);
    }

    int addInstrumentedMethod(AbstractTracingMethodAdapter methodAdapter) {
        int index = this.instrumentedMethods.size();
        this.instrumentedMethods.add(methodAdapter);
        return index;
    }

    public Collection<AbstractTracingMethodAdapter> getInstrumentedMethods() {
        return this.instrumentedMethods;
    }

    private class InitMethod extends AdviceAdapter {
        private InitMethod(MethodVisitor mv, int access, String name, String desc) {
            super(Agent.ASM_LEVEL, mv, access, name, desc);
        }

        private int getAgentWrapper() {
            (new MethodBuilder(this, this.methodAccess)).loadInvocationHandlerFromProxy();
            int invocationHandlerVar = this.newLocal(MethodBuilder.INVOCATION_HANDLER_TYPE);
            this.mv.visitVarInsn(58, invocationHandlerVar);
            return invocationHandlerVar;
        }

        protected void onMethodEnter() {
            if (GenericClassAdapter.this.classBeingRedefined == null) {
                int invocationHandlerVar = this.getAgentWrapper();
                this.visitLdcInsn(Type.getObjectType(GenericClassAdapter.this.className));
                int classVar = this.newLocal(Type.getType(Object.class));
                this.mv.visitVarInsn(58, classVar);
                if (GenericClassAdapter.this.canModifyClassStructure()) {
                    this.push(GenericClassAdapter.this.instrumentedMethods.size());
                    this.newArray(MethodBuilder.INVOCATION_HANDLER_TYPE);
                    this.putStatic(Type.getObjectType(GenericClassAdapter.this.className), "__nr__InvocationHandlers",
                                          MethodBuilder.INVOCATION_HANDLER_ARRAY_TYPE);
                    Iterator i$ = GenericClassAdapter.this.instrumentedMethods.iterator();

                    while (i$.hasNext()) {
                        AbstractTracingMethodAdapter methodAdapter = (AbstractTracingMethodAdapter) i$.next();
                        if (methodAdapter.getInvocationHandlerIndex() >= 0) {
                            this.initMethod(classVar, invocationHandlerVar, methodAdapter);
                        }
                    }
                }

            }
        }

        private void initMethod(int classVar, int invocationHandlerVar, AbstractTracingMethodAdapter methodAdapter) {
            this.getStatic(Type.getObjectType(GenericClassAdapter.this.className), "__nr__InvocationHandlers",
                                  MethodBuilder.INVOCATION_HANDLER_ARRAY_TYPE);
            this.push(methodAdapter.getInvocationHandlerIndex());
            this.mv.visitVarInsn(25, invocationHandlerVar);
            this.mv.visitVarInsn(25, classVar);
            this.visitInsn(1);
            ArrayList<Object> arguments =
                    new ArrayList<Object>(Arrays.asList(GenericClassAdapter.this.className, methodAdapter.methodName,
                                                               methodAdapter.getMethodDescriptor(),
                                                               Boolean.valueOf(false), Boolean.valueOf(false)));
            (new MethodBuilder(this, this.methodAccess))
                    .loadArray(Object.class, arguments.toArray(new Object[arguments.size()]))
                    .invokeInvocationHandlerInterface(false);
            this.checkCast(MethodBuilder.INVOCATION_HANDLER_TYPE);
            this.arrayStore(MethodBuilder.INVOCATION_HANDLER_TYPE);
        }
    }

    private class InitMethodAdapter extends AdviceAdapter {
        protected InitMethodAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(Agent.ASM_LEVEL, mv, access, name, desc);
        }

        protected void onMethodEnter() {
            this.mv.visitMethodInsn(184, GenericClassAdapter.this.className, INIT_CLASS_METHOD_NAME, NO_ARG_VOID_DESC,
                                           false);
        }
    }
}
