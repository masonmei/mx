package com.newrelic.agent.profile;

public abstract interface ProfilingTaskController extends ProfilingTask
{
  public abstract int getSamplePeriodInMillis();
}