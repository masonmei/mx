package com.newrelic.agent.instrumentation.tracing;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.agent.bridge.NoOpTracedMethod;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ClassMethodSignatures;
import com.newrelic.agent.util.asm.BytecodeGenProxyBuilder;
import com.newrelic.agent.util.asm.Variables;

public class TraceMethodVisitor extends AdviceAdapter {
    public static final Method IGNORE_APDEX_METHOD = new Method("ignoreApdex", Type.VOID_TYPE, new Type[0]);
    static final Type TRACER_TYPE = Type.getType(ExitTracer.class);
    static final Type THROWABLE_TYPE = Type.getType(Throwable.class);
    protected final Method method;
    protected final String className;
    private final int tracerLocal;
    private final Label startFinallyLabel;
    private final TraceDetails traceDetails;
    private final int access;
    private final boolean customTracer;
    private final int signatureId;

    public TraceMethodVisitor(String className, MethodVisitor mv, int access, String name, String desc,
                              TraceDetails trace, boolean customTracer, Class<?> classBeingRedefined) {
        super(Agent.ASM_LEVEL, mv, access, name, desc);

        this.className = className.replace('/', '.');
        method = new Method(name, desc);
        this.access = access;
        this.customTracer = customTracer;

        startFinallyLabel = new Label();
        tracerLocal = newLocal(TRACER_TYPE);
        traceDetails = trace;

        int signatureId = -1;
        ClassMethodSignature signature =
                new ClassMethodSignature(this.className.intern(), method.getName().intern(), methodDesc.intern());

        if (classBeingRedefined != null) {
            signatureId = ClassMethodSignatures.get().getIndex(signature);
        }
        if (signatureId == -1) {
            signatureId = ClassMethodSignatures.get().add(signature);
        }
        this.signatureId = signatureId;
    }

    protected void onMethodEnter() {
        super.onMethodEnter();

        startTracer();
    }

    protected void startTracer() {
        visitInsn(1);
        storeLocal(tracerLocal, TRACER_TYPE);

        visitLabel(startFinallyLabel);

        Label start = new Label();
        Label end = new Label();

        visitLabel(start);

        super.getStatic(BridgeUtils.AGENT_BRIDGE_TYPE, "instrumentation", BridgeUtils.INSTRUMENTATION_TYPE);

        String metricName = traceDetails.getFullMetricName(className, method.getName());

        String tracerFactory = traceDetails.tracerFactoryName();

        BytecodeGenProxyBuilder builder = BytecodeGenProxyBuilder.newBuilder(Instrumentation.class, this, true);

        Variables loader = builder.getVariables();
        Instrumentation instrumentation = (Instrumentation) builder.build();

        if (tracerFactory == null) {
            int tracerFlags = getTracerFlags();

            instrumentation.createTracer(loader.loadThis(access), signatureId, metricName, tracerFlags);
        } else {
            Object[] loadArgs = (Object[]) loader.load(Object.class, new Runnable() {
                public void run() {
                    loadArgArray();
                }
            });
            instrumentation.createTracer(loader.loadThis(access), signatureId, traceDetails.dispatcher(), metricName,
                                                tracerFactory, loadArgs);
        }

        storeLocal(tracerLocal, TRACER_TYPE);

        goTo(end);

        Label handler = new Label();
        visitLabel(handler);

        pop();

        visitLabel(end);
        visitTryCatchBlock(start, end, handler, THROWABLE_TYPE.getInternalName());
    }

    private int getTracerFlags() {
        int tracerFlags = 2;
        if (traceDetails.dispatcher()) {
            tracerFlags |= 8;
        }
        if (traceDetails.isLeaf()) {
            tracerFlags |= 32;
        }
        if (!traceDetails.excludeFromTransactionTrace()) {
            tracerFlags |= 4;
        }
        if (customTracer) {
            tracerFlags |= 16;
        }
        return tracerFlags;
    }

    public void visitMaxs(int maxStack, int maxLocals) {
        Label endFinallyLabel = new Label();
        super.visitTryCatchBlock(startFinallyLabel, endFinallyLabel, endFinallyLabel, THROWABLE_TYPE.getInternalName());

        super.visitLabel(endFinallyLabel);

        onEveryExit(191);
        super.visitInsn(191);

        super.visitMaxs(maxStack, maxLocals);
    }

    protected void onMethodExit(int opcode) {
        if (opcode != 191) {
            onEveryExit(opcode);
        }
    }

    protected void onEveryExit(int opcode) {
        Label isTracerNullLabel = new Label();

        loadLocal(tracerLocal);
        ifNull(isTracerNullLabel);

        if (191 == opcode) {
            dup();
        }

        loadLocal(tracerLocal);

        ExitTracer tracer = (ExitTracer) BytecodeGenProxyBuilder.newBuilder(ExitTracer.class, this, false).build();
        if (191 == opcode) {
            swap();
            tracer.finish(null);
        } else {
            push(opcode);
            visitInsn(1);
            tracer.finish(0, null);
        }

        visitLabel(isTracerNullLabel);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (owner.equals(BridgeUtils.TRACED_METHOD_TYPE.getInternalName())) {
            loadTracer();
        } else {
            super.visitFieldInsn(opcode, owner, name, desc);
        }
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if ((BridgeUtils.isAgentType(owner)) && ("getTracedMethod".equals(name))) {
            pop();

            loadTracer();
        } else {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    private void loadTracer() {
        Label isTracerNullLabel = new Label();
        Label end = new Label();

        loadLocal(tracerLocal);
        ifNull(isTracerNullLabel);

        loadLocal(tracerLocal);
        goTo(end);

        visitLabel(isTracerNullLabel);

        getStatic(Type.getType(NoOpTracedMethod.class), "INSTANCE", Type.getType(TracedMethod.class));

        visitLabel(end);
    }
}