package com.newrelic.agent.environment;

import com.newrelic.agent.service.Service;

public abstract interface EnvironmentService extends Service
{
  public abstract int getProcessPID();

  public abstract Environment getEnvironment();
}