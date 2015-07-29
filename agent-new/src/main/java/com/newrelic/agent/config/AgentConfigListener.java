package com.newrelic.agent.config;

public abstract interface AgentConfigListener
{
  public abstract void configChanged(String paramString, AgentConfig paramAgentConfig);
}