package com.newrelic.agent.config;

import java.util.Set;

public abstract interface ErrorCollectorConfig
{
  public abstract boolean isEnabled();

  public abstract Set<Integer> getIgnoreStatusCodes();

  public abstract Set<String> getIgnoreErrors();

  public abstract <T> T getProperty(String paramString);

  public abstract <T> T getProperty(String paramString, T paramT);
}