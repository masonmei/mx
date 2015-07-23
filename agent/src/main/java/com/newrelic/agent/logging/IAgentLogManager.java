package com.newrelic.agent.logging;

import com.newrelic.agent.config.AgentConfig;

public interface IAgentLogManager {
    IAgentLogger getRootLogger();

    String getLogFilePath();

    void configureLogger(AgentConfig paramAgentConfig);

    void addConsoleHandler();

    String getLogLevel();

    void setLogLevel(String paramString);
}