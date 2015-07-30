package com.newrelic.agent.tracers;

public interface EntryInvocationHandler extends PointCutInvocationHandler {
    void handleInvocation(ClassMethodSignature paramClassMethodSignature, Object paramObject,
                          Object[] paramArrayOfObject);
}