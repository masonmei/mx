package com.newrelic.agent.profile;

import com.newrelic.deps.org.json.simple.JSONStreamAware;

public abstract interface IProfile extends JSONStreamAware
{
  public abstract void start();

  public abstract void end();

  public abstract void beforeSampling();

  public abstract void addStackTrace(long paramLong, boolean paramBoolean, ThreadType paramThreadType,
                                     StackTraceElement[] paramArrayOfStackTraceElement);

  public abstract ProfilerParameters getProfilerParameters();

  public abstract int getSampleCount();

  public abstract Long getProfileId();

  public abstract ProfileTree getProfileTree(ThreadType paramThreadType);

  public abstract int trimBy(int paramInt);

  public abstract long getStartTimeMillis();

  public abstract long getEndTimeMillis();

  public abstract void markInstrumentedMethods();
}