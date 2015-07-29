package com.newrelic.agent.service;

import com.newrelic.agent.logging.IAgentLogger;

public abstract interface Service
{
  public abstract String getName();

  public abstract void start()
    throws Exception;

  public abstract void stop()
    throws Exception;

  public abstract boolean isEnabled();

  public abstract IAgentLogger getLogger();

  public abstract boolean isStarted();

  public abstract boolean isStopped();

  public abstract boolean isStartedOrStarting();

  public abstract boolean isStoppedOrStopping();
}