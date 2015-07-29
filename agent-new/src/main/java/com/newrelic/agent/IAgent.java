package com.newrelic.agent;

import com.newrelic.agent.service.Service;

public abstract interface IAgent extends Service
{
  public abstract InstrumentationProxy getInstrumentation();

  public abstract void shutdownAsync();

  public abstract void shutdown();
}