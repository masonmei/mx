//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.tracing;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.util.Strings;
import com.newrelic.agent.util.asm.BytecodeGenProxyBuilder;
import com.newrelic.agent.util.asm.Variables;

public class FlyweightTraceMethodVisitor extends AdviceAdapter {
    static final Type THROWABLE_TYPE = Type.getType(Throwable.class);
    private static final Type JOINER_TYPE = Type.getType(Joiner.class);
    final Map<Method, FlyweightTraceMethodVisitor.Handler> tracedMethodMethodHandlers;
    private final Method method;
    private final int startTimeLocal;
    private final Label startFinallyLabel;
    private final TraceDetails traceDetails;
    private final String className;
    private final int parentTracerLocal;
    private final int metricNameLocal;
    private final int rollupMetricNamesCacheId;

    public FlyweightTraceMethodVisitor(String className, MethodVisitor mv, int access, String name, String desc,
                                       TraceDetails trace, Class<?> classBeingRedefined) {
        super(Agent.ASM_LEVEL, mv, access, name, desc);
        this.className = className.replace('/', '.');
        this.method = new Method(name, desc);
        this.startFinallyLabel = new Label();
        this.startTimeLocal = this.newLocal(Type.LONG_TYPE);
        this.parentTracerLocal = this.newLocal(BridgeUtils.TRACED_METHOD_TYPE);
        this.metricNameLocal = this.newLocal(Type.getType(String.class));
        if (trace.rollupMetricName().length > 0) {
            this.rollupMetricNamesCacheId = AgentBridge.instrumentation.addToObjectCache(trace.rollupMetricName());
        } else {
            this.rollupMetricNamesCacheId = -1;
        }

        this.traceDetails = trace;
        this.tracedMethodMethodHandlers = this.getTracedMethodMethodHandlers();
    }

    private Map<Method, FlyweightTraceMethodVisitor.Handler> getTracedMethodMethodHandlers() {
        HashMap map = Maps.newHashMap();
        map.put(new Method("getMetricName", "()Ljava/lang/String;"), new FlyweightTraceMethodVisitor.Handler() {
            public void handle(AdviceAdapter mv) {
                mv.loadLocal(FlyweightTraceMethodVisitor.this.metricNameLocal);
            }
        });
        map.put(new Method("setMetricName", "([Ljava/lang/String;)V"), new FlyweightTraceMethodVisitor.Handler() {
            public void handle(AdviceAdapter mv) {
                mv.checkCast(Type.getType(Object[].class));
                FlyweightTraceMethodVisitor.this.push("");
                mv.invokeStatic(FlyweightTraceMethodVisitor.JOINER_TYPE,
                                       new Method("on", FlyweightTraceMethodVisitor.JOINER_TYPE,
                                                         new Type[] {Type.getType(String.class)}));
                mv.swap();
                mv.invokeVirtual(FlyweightTraceMethodVisitor.JOINER_TYPE, new Method("join", Type.getType(String.class),
                                                                                            new Type[] {Type.getType(Object[].class)}));
                mv.storeLocal(FlyweightTraceMethodVisitor.this.metricNameLocal);
            }
        });
        this.addUnsupportedMethod(map, new Method("nameTransaction", Type.VOID_TYPE,
                                                         new Type[] {Type.getType(TransactionNamePriority.class)}));
        this.addUnsupportedMethod(map, new Method("setRollupMetricNames", "([Ljava/lang/String;)V"));
        this.addUnsupportedMethod(map, new Method("addRollupMetricName", "([Ljava/lang/String;)V"));
        this.addUnsupportedMethod(map, new Method("setMetricNameFormatInfo",
                                                         "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"));
        this.addUnsupportedMethod(map, new Method("addExclusiveRollupMetricName", "([Ljava/lang/String;)V"));
        map.put(new Method("getParentTracedMethod", "()Lcom/newrelic/agent/bridge/TracedMethod;"),
                       new FlyweightTraceMethodVisitor.Handler() {
                           public void handle(AdviceAdapter mv) {
                               mv.loadLocal(FlyweightTraceMethodVisitor.this.parentTracerLocal);
                           }
                       });
        return map;
    }

    private void addUnsupportedMethod(Map<Method, FlyweightTraceMethodVisitor.Handler> map, Method method) {
        map.put(method, new FlyweightTraceMethodVisitor.UnsupportedHandler(method));
    }

    protected void onMethodEnter() {
        super.onMethodEnter();
        this.push(0L);
        super.storeLocal(this.startTimeLocal, Type.LONG_TYPE);
        this.visitInsn(1);
        super.storeLocal(this.parentTracerLocal, BridgeUtils.TRACED_METHOD_TYPE);
        this.visitInsn(1);
        super.storeLocal(this.metricNameLocal);
        Label start = new Label();
        Label end = new Label();
        this.visitLabel(start);
        super.invokeStatic(Type.getType(System.class), new Method("nanoTime", Type.LONG_TYPE, new Type[0]));
        super.storeLocal(this.startTimeLocal, Type.LONG_TYPE);
        BridgeUtils.getCurrentTransaction(this);
        Transaction transactionApi =
                (Transaction) BytecodeGenProxyBuilder.newBuilder(Transaction.class, this, true).build();
        transactionApi.startFlyweightTracer();
        super.storeLocal(this.parentTracerLocal, BridgeUtils.TRACED_METHOD_TYPE);
        String fullMetricName = this.traceDetails.getFullMetricName(this.className, this.method.getName());
        if (fullMetricName == null) {
            fullMetricName = Strings.join('/', new String[] {"Java", this.className, this.method.getName()});
        }

        this.push(fullMetricName);
        super.storeLocal(this.metricNameLocal);
        this.goTo(end);
        Label handler = new Label();
        this.visitLabel(handler);
        this.pop();
        this.visitLabel(end);
        this.visitTryCatchBlock(start, end, handler, TraceMethodVisitor.THROWABLE_TYPE.getInternalName());
        super.visitLabel(this.startFinallyLabel);
    }

    public void visitMaxs(int maxStack, int maxLocals) {
        Label endFinallyLabel = new Label();
        super.visitTryCatchBlock(this.startFinallyLabel, endFinallyLabel, endFinallyLabel,
                                        THROWABLE_TYPE.getInternalName());
        super.visitLabel(endFinallyLabel);
        this.onEveryExit(191);
        super.visitInsn(191);
        super.visitMaxs(maxStack, maxLocals);
    }

    protected void onMethodExit(int opcode) {
        if (opcode != 191) {
            this.onEveryExit(opcode);
        }

    }

    protected void onEveryExit(int opcode) {
        Label skip = super.newLabel();
        super.loadLocal(this.parentTracerLocal);
        super.ifNull(skip);
        BridgeUtils.getCurrentTransaction(this);
        super.ifNull(skip);
        BridgeUtils.getCurrentTransaction(this);
        BytecodeGenProxyBuilder builder = BytecodeGenProxyBuilder.newBuilder(Transaction.class, this, true);
        Variables loader = builder.getVariables();
        String[] rollupMetricNames;
        if (this.rollupMetricNamesCacheId >= 0) {
            rollupMetricNames = (String[]) loader.load(String[].class, new Runnable() {
                public void run() {
                    FlyweightTraceMethodVisitor.this.getStatic(BridgeUtils.AGENT_BRIDGE_TYPE, "instrumentation",
                                                                      BridgeUtils.INSTRUMENTATION_TYPE);
                    ((Instrumentation) BytecodeGenProxyBuilder
                                               .newBuilder(Instrumentation.class, FlyweightTraceMethodVisitor.this,
                                                                  true).build())
                            .getCachedObject(FlyweightTraceMethodVisitor.this.rollupMetricNamesCacheId);
                    FlyweightTraceMethodVisitor.this.checkCast(Type.getType(String[].class));
                }
            });
        } else {
            rollupMetricNames = null;
        }

        long startTime = ((Long) loader.loadLocal(this.startTimeLocal, Type.LONG_TYPE, Long.valueOf(-1L))).longValue();
        long loadEndTime = ((Long) loader.load(Long.valueOf(-2L), new Runnable() {
            public void run() {
                FlyweightTraceMethodVisitor.this
                        .invokeStatic(Type.getType(System.class), new Method("nanoTime", Type.LONG_TYPE, new Type[0]));
            }
        })).longValue();
        Transaction transactionApi = (Transaction) builder.build();
        transactionApi
                .finishFlyweightTracer((TracedMethod) loader.loadLocal(this.parentTracerLocal, TracedMethod.class),
                                              startTime, loadEndTime, this.className, this.method.getName(),
                                              this.methodDesc,
                                              (String) loader.loadLocal(this.metricNameLocal, String.class),
                                              rollupMetricNames);
        super.visitLabel(skip);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (!owner.equals(BridgeUtils.TRACED_METHOD_TYPE.getInternalName())) {
            super.visitFieldInsn(opcode, owner, name, desc);
        }

    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (BridgeUtils.isAgentType(owner) && "getTracedMethod".equals(name)) {
            this.pop();
        } else if (BridgeUtils.isTracedMethodType(owner)) {
            Method method = new Method(name, desc);
            FlyweightTraceMethodVisitor.Handler handler =
                    (FlyweightTraceMethodVisitor.Handler) this.tracedMethodMethodHandlers.get(method);
            if (handler != null) {
                handler.handle(this);
            }
        } else {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }

    }

    private interface Handler {
        void handle(AdviceAdapter var1);
    }

    private static class UnsupportedHandler implements FlyweightTraceMethodVisitor.Handler {
        private final Method method;

        public UnsupportedHandler(Method method) {
            this.method = method;
        }

        public void handle(AdviceAdapter mv) {
            Agent.LOG.log(Level.FINER, "{0}.{1} is unsupported in flyweight tracers",
                                 new Object[] {TracedMethod.class.getSimpleName(), this.method});
        }
    }
}
