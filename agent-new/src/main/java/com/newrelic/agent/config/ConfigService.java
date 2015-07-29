package com.newrelic.agent.config;

import com.newrelic.agent.service.Service;
import java.util.Map;

public abstract interface ConfigService extends Service
{
  public abstract void addIAgentConfigListener(AgentConfigListener paramAgentConfigListener);

  public abstract void removeIAgentConfigListener(AgentConfigListener paramAgentConfigListener);

  public abstract Map<String, Object> getLocalSettings();

  public abstract Map<String, Object> getSanitizedLocalSettings();

  public abstract AgentConfig getDefaultAgentConfig();

  public abstract AgentConfig getLocalAgentConfig();

  public abstract AgentConfig getAgentConfig(String paramString);

  public abstract TransactionTracerConfig getTransactionTracerConfig(String paramString);

  public abstract ErrorCollectorConfig getErrorCollectorConfig(String paramString);

  public abstract JarCollectorConfig getJarCollectorConfig(String paramString);

  public abstract StripExceptionConfig getStripExceptionConfig(String paramString);
}