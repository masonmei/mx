package com.newrelic.agent.instrumentation;

import com.newrelic.agent.bridge.AgentBridge;

public class ClassLoaderCheck {
    private static final String CLASSNAME = AgentBridge.class.getName();

    public static void loadAgentClass(ClassLoader loader) throws Throwable {
        if (loader != null) {
            loader.loadClass(CLASSNAME);
        }
    }
}