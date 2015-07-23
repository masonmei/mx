package com.newrelic.agent.jmx;

public abstract interface AgentMBean {
    public abstract boolean shutdown();

    public abstract boolean reconnect();

    public abstract boolean connect();

    public abstract String setLogLevel(String paramString);

    public abstract String getLogLevel();

    public abstract boolean isStarted();

    public abstract boolean isConnected();
}