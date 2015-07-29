package com.newrelic.agent.tracers;

public abstract interface EntryInvocationHandler extends PointCutInvocationHandler
{
  public abstract void handleInvocation(ClassMethodSignature paramClassMethodSignature, Object paramObject,
                                        Object[] paramArrayOfObject);
}