package com.newrelic.agent.profile;

public abstract interface ProfilerControl
{
  public abstract void startProfiler(ProfilerParameters paramProfilerParameters);

  public abstract int stopProfiler(Long paramLong, boolean paramBoolean);

  public abstract boolean isEnabled();
}