package com.newrelic.agent;

import com.newrelic.agent.service.Service;

public abstract interface HarvestService extends Service
{
  public abstract void startHarvest(IRPMService paramIRPMService);

  public abstract void addHarvestListener(HarvestListener paramHarvestListener);

  public abstract void removeHarvestListener(HarvestListener paramHarvestListener);

  public abstract void harvestNow();
}