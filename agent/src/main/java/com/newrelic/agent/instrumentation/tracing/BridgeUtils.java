package com.newrelic.agent.instrumentation.tracing;

import java.util.Set;
import java.util.logging.Level;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.agent.bridge.PrivateApi;
import com.newrelic.agent.bridge.PublicApi;
import com.newrelic.agent.util.asm.BytecodeGenProxyBuilder;
import com.newrelic.agent.util.asm.VariableLoader;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weaver;

public class BridgeUtils {
    public static final Type NEW_RELIC_API_TYPE = Type.getType(NewRelic.class);

    public static final Type PRIVATE_API_TYPE = Type.getType(PrivateApi.class);

    public static final Type PUBLIC_API_TYPE = Type.getType(PublicApi.class);

    public static final Type AGENT_BRIDGE_TYPE = Type.getType(AgentBridge.class);

    public static final Type TRACED_METHOD_TYPE = Type.getType(com.newrelic.agent.bridge.TracedMethod.class);

    public static final Type PUBLIC_AGENT_TYPE = Type.getType(com.newrelic.api.agent.Agent.class);

    public static final Type INTERNAL_AGENT_TYPE = Type.getType(com.newrelic.agent.bridge.Agent.class);

    public static final Type INSTRUMENTATION_TYPE = Type.getType(Instrumentation.class);

    public static final Type TRANSACTION_TYPE = Type.getType(com.newrelic.agent.bridge.Transaction.class);
    public static final String PRIVATE_API_FIELD_NAME = "privateApi";
    public static final String PUBLIC_API_FIELD_NAME = "publicApi";
    public static final String INSTRUMENTATION_FIELD_NAME = "instrumentation";
    public static final String GET_TRACED_METHOD_METHOD_NAME = "getTracedMethod";
    public static final String GET_TRANSACTION_METHOD_NAME = "getTransaction";
    public static final Type WEAVER_TYPE = Type.getType(Weaver.class);
    public static final String CURRENT_TRANSACTION_FIELD_NAME = "CURRENT";
    private static final String AGENT_FIELD_NAME = "agent";
    private static final String GET_LOGGER_METHOD_NAME = "getLogger";
    private static final Type LOGGER_TYPE = Type.getType(Logger.class);
    private static final Set<String> AGENT_CLASS_NAMES =
            ImmutableSet.of(PUBLIC_AGENT_TYPE.getInternalName(), INTERNAL_AGENT_TYPE.getInternalName());

    private static final Set<String> TRACED_METHOD_CLASS_NAMES = ImmutableSet.of(TRACED_METHOD_TYPE.getInternalName(),
                                                                                        Type.getInternalName(com.newrelic.api.agent.TracedMethod.class));

    private static final Set<String> TRANSACTION_CLASS_NAMES = ImmutableSet.of(TRANSACTION_TYPE.getInternalName(),
                                                                                      Type.getInternalName(com.newrelic.api.agent.Transaction.class),
                                                                                      Type.getInternalName(com.newrelic.agent.Transaction.class));

    public static void loadLogger(GeneratorAdapter mv) {
        mv.visitFieldInsn(178, AGENT_BRIDGE_TYPE.getInternalName(), "agent", INTERNAL_AGENT_TYPE.getDescriptor());

        mv.invokeInterface(PUBLIC_AGENT_TYPE, new Method("getLogger", LOGGER_TYPE, new Type[0]));
    }

    public static BytecodeGenProxyBuilder<Logger> getLoggerBuilder(GeneratorAdapter mv, boolean loadArgs) {
        BytecodeGenProxyBuilder builder = BytecodeGenProxyBuilder.newBuilder(Logger.class, mv, loadArgs);

        if (loadArgs) {
            builder.addLoader(Type.getType(Level.class), new VariableLoader() {
                public void load(Object value, GeneratorAdapter methodVisitor) {
                    methodVisitor
                            .getStatic(Type.getType(Level.class), ((Level) value).getName(), Type.getType(Level.class));
                }

            });
        }

        return builder;
    }

    public static Logger getLogger(GeneratorAdapter mv) {
        loadLogger(mv);

        return (Logger) getLoggerBuilder(mv, true).build();
    }

    public static void getCurrentTransaction(MethodVisitor mv) {
        mv.visitFieldInsn(178, TRANSACTION_TYPE.getInternalName(), "CURRENT", TRANSACTION_TYPE.getDescriptor());
    }

    public static boolean isAgentType(String owner) {
        return AGENT_CLASS_NAMES.contains(owner);
    }

    public static boolean isTracedMethodType(String owner) {
        return TRACED_METHOD_CLASS_NAMES.contains(owner);
    }

    public static boolean isTransactionType(String owner) {
        return TRANSACTION_CLASS_NAMES.contains(owner);
    }
}