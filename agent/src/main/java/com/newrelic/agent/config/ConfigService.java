package com.newrelic.agent.config;

import java.util.Map;

import com.newrelic.agent.service.Service;

public interface ConfigService extends Service {
    void addIAgentConfigListener(AgentConfigListener paramAgentConfigListener);

    void removeIAgentConfigListener(AgentConfigListener paramAgentConfigListener);

    Map<String, Object> getLocalSettings();

    Map<String, Object> getSanitizedLocalSettings();

    AgentConfig getDefaultAgentConfig();

    AgentConfig getLocalAgentConfig();

    AgentConfig getAgentConfig(String paramString);

    TransactionTracerConfig getTransactionTracerConfig(String paramString);

    ErrorCollectorConfig getErrorCollectorConfig(String paramString);

    JarCollectorConfig getJarCollectorConfig(String paramString);

    StripExceptionConfig getStripExceptionConfig(String paramString);
}