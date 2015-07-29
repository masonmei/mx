package com.newrelic.agent.config;

public abstract interface JarCollectorConfig
{
  public abstract boolean isEnabled();

  public abstract int getMaxClassLoaders();
}