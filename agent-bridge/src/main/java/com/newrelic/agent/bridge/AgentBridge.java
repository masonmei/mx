package com.newrelic.agent.bridge;

import java.lang.reflect.InvocationHandler;

import com.newrelic.api.agent.MetricAggregator;

public final class AgentBridge {
    public static final Class<?>[] API_CLASSES =
            {PrivateApi.class, TracedMethod.class, Instrumentation.class, AsyncApi.class, Transaction.class,
                    JmxApi.class, MetricAggregator.class};

    public static volatile PublicApi publicApi = new NoOpPublicApi();

    public static volatile PrivateApi privateApi = new NoOpPrivateApi();

    public static volatile ObjectFieldManager objectFieldManager = new NoOpObjectFieldManager();

    public static volatile JmxApi jmxApi = new NoOpJmxApi();

    public static volatile Instrumentation instrumentation = new NoOpInstrumentation();

    public static volatile AsyncApi asyncApi = new NoOpAsyncApi();
    public static volatile InvocationHandler agentHandler;
    public static volatile Agent agent = NoOpAgent.INSTANCE;

    public static Agent getAgent() {
        return agent;
    }
}