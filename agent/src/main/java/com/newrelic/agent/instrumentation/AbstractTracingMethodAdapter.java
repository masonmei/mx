package com.newrelic.agent.instrumentation;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.deps.org.objectweb.asm.Label;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.AdviceAdapter;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

abstract class AbstractTracingMethodAdapter extends AdviceAdapter {
    private static final String JAVA_LANG_THROWABLE = "java/lang/Throwable";
    private static final boolean sDebugTracers = false;
    protected final String methodName;
    protected final GenericClassAdapter genericClassAdapter;
    protected final MethodBuilder methodBuilder;
    private final Label startFinallyLabel = new Label();
    private int tracerLocalId;
    private int invocationHandlerIndex = -1;

    public AbstractTracingMethodAdapter(GenericClassAdapter genericClassAdapter, MethodVisitor mv, int access,
                                        Method method) {
        super(Agent.ASM_LEVEL, mv, access, method.getName(), method.getDescriptor());
        this.genericClassAdapter = genericClassAdapter;
        methodName = method.getName();
        methodBuilder = new MethodBuilder(this, access);
    }

    String getMethodDescriptor() {
        return methodDesc;
    }

    protected void systemOutPrint(String message) {
        systemPrint(message, sDebugTracers);
    }

    protected void systemPrint(String message, boolean error) {
        getStatic(Type.getType(System.class), error ? "err" : "out", Type.getType(PrintStream.class));
        visitLdcInsn(message);
        invokeVirtual(Type.getType(PrintStream.class), new Method("println", "(Ljava/lang/String;)V"));
    }

    protected void onMethodEnter() {
        int methodIndex = getGenericClassAdapter().addInstrumentedMethod(this);
        if (getGenericClassAdapter().canModifyClassStructure()) {
            setInvocationFieldIndex(methodIndex);
        }

        try {
            Type tracerType = getTracerType();
            tracerLocalId = newLocal(tracerType);

            visitInsn(1);
            storeLocal(tracerLocalId);

            Label startLabel = new Label();
            Label endLabel = new Label();
            Label exceptionLabel = new Label();

            mv.visitTryCatchBlock(startLabel, endLabel, exceptionLabel, JAVA_LANG_THROWABLE);
            mv.visitLabel(startLabel);
            loadGetTracerArguments();
            invokeGetTracer();

            storeLocal(tracerLocalId);
            mv.visitLabel(endLabel);
            Label doneLabel = new Label();
            goTo(doneLabel);
            mv.visitLabel(exceptionLabel);
            if (Agent.LOG.isLoggable(Level.FINER)) {
                mv.visitMethodInsn(182, JAVA_LANG_THROWABLE, "printStackTrace", "()V", sDebugTracers);
                systemPrint(MessageFormat.format("An error occurred creating a tracer for {0}.{1}{2}",
                                                        getGenericClassAdapter().className, methodName, methodDesc),
                                   true);
            } else {
                int exceptionVar = newLocal(Type.getType(Throwable.class));
                visitVarInsn(58, exceptionVar);
            }
            mv.visitLabel(doneLabel);
        } catch (Throwable e) {
            Agent.LOG.severe(MessageFormat.format("An error occurred transforming {0}.{1}{2} : {3}",
                                                         getGenericClassAdapter().className, methodName, methodDesc,
                                                         e.toString()));

            throw new RuntimeException(e);
        }
    }

    private void setInvocationFieldIndex(int id) {
        invocationHandlerIndex = id;
    }

    public int getInvocationHandlerIndex() {
        return invocationHandlerIndex;
    }

    protected final Type getTracerType() {
        return MethodBuilder.INVOCATION_HANDLER_TYPE;
    }

    protected final void invokeGetTracer() {
        methodBuilder.invokeInvocationHandlerInterface(sDebugTracers);
    }

    protected abstract void loadGetTracerArguments();

    public GenericClassAdapter getGenericClassAdapter() {
        return genericClassAdapter;
    }

    public void visitCode() {
        super.visitCode();
        super.visitLabel(startFinallyLabel);
    }

    public void visitMaxs(int maxStack, int maxLocals) {
        Label endFinallyLabel = new Label();
        super.visitTryCatchBlock(startFinallyLabel, endFinallyLabel, endFinallyLabel, JAVA_LANG_THROWABLE);
        super.visitLabel(endFinallyLabel);
        onFinally(191);
        super.visitInsn(191);
        super.visitMaxs(maxStack, maxLocals);
    }

    protected void onMethodExit(int opcode) {
        if (opcode != 191) {
            onFinally(opcode);
        }
    }

    protected void onFinally(int opcode) {
        Label end = new Label();
        if (opcode == 191) {
            if ("<init>".equals(methodName)) {
                return;
            }
            dup();
            int exceptionVar = newLocal(Type.getType(Throwable.class));
            visitVarInsn(58, exceptionVar);

            loadLocal(tracerLocalId);
            ifNull(end);
            loadLocal(tracerLocalId);

            checkCast(MethodBuilder.INVOCATION_HANDLER_TYPE);

            invokeTraceFinishWithThrowable(exceptionVar);
        } else {
            Object loadReturnValue = null;
            if (opcode != 177) {
                loadReturnValue = new StoreReturnValueAndReload(opcode);
            }

            loadLocal(tracerLocalId);
            ifNull(end);
            loadLocal(tracerLocalId);

            invokeTraceFinish(opcode, loadReturnValue);
        }
        visitLabel(end);
    }

    protected final void invokeTraceFinish(int opcode, Object loadReturnValue) {
        methodBuilder.loadSuccessful().loadArray(Object.class, opcode, loadReturnValue)
                .invokeInvocationHandlerInterface(true);
    }

    protected final void invokeTraceFinishWithThrowable(final int exceptionVar) {
        methodBuilder.loadUnsuccessful().loadArray(Object.class, new Runnable() {
            public void run() {
                visitVarInsn(25, exceptionVar);
            }
        }).invokeInvocationHandlerInterface(true);
    }

    private final class StoreReturnValueAndReload implements Runnable {
        private final int returnVar;

        public StoreReturnValueAndReload(int opcode) {
            Type returnType = Type.getReturnType(methodDesc);

            if (returnType.getSize() == 2) {
                dup2();
            } else {
                dup();
            }
            returnType = methodBuilder.box(returnType);

            returnVar = newLocal(returnType);
            storeLocal(returnVar, returnType);
        }

        public void run() {
            loadLocal(returnVar);
        }
    }
}