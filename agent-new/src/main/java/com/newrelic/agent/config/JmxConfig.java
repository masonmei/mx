package com.newrelic.agent.config;

import java.util.Collection;

public abstract interface JmxConfig
{
  public abstract boolean isEnabled();

  public abstract boolean isCreateMbeanServer();

  public abstract Collection<String> getDisabledJmxFrameworks();
}