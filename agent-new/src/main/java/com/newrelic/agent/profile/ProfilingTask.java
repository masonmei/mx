package com.newrelic.agent.profile;

import com.newrelic.agent.HarvestListener;

public abstract interface ProfilingTask extends Runnable, HarvestListener
{
  public abstract void addProfile(ProfilerParameters paramProfilerParameters);

  public abstract void removeProfile(ProfilerParameters paramProfilerParameters);
}