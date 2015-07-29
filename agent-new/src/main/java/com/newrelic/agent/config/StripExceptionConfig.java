package com.newrelic.agent.config;

import java.util.Set;

public abstract interface StripExceptionConfig
{
  public abstract boolean isEnabled();

  public abstract Set<String> getWhitelist();
}