package com.newrelic.agent.rpm;

import com.newrelic.agent.IRPMService;
import com.newrelic.agent.service.Service;

public abstract interface RPMConnectionService extends Service
{
  public abstract void connect(IRPMService paramIRPMService);

  public abstract void connectImmediate(IRPMService paramIRPMService);
}