package com.newrelic.agent.config;

public abstract interface ReinstrumentConfig
{
  public abstract boolean isEnabled();

  public abstract boolean isAttributesEnabled();
}