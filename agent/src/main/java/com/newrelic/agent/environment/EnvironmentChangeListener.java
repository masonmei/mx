package com.newrelic.agent.environment;

public abstract interface EnvironmentChangeListener {
    public abstract void agentIdentityChanged(AgentIdentity paramAgentIdentity);
}