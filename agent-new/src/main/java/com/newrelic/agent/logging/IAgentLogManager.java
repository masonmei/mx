package com.newrelic.agent.logging;

import com.newrelic.agent.config.AgentConfig;

public abstract interface IAgentLogManager {
    public abstract IAgentLogger getRootLogger();

    public abstract String getLogFilePath();

    public abstract void configureLogger(AgentConfig paramAgentConfig);

    public abstract void addConsoleHandler();

    public abstract String getLogLevel();

    public abstract void setLogLevel(String paramString);
}