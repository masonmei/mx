package com.newrelic.agent.tracers;

import com.newrelic.agent.Transaction;

public abstract interface TracerFactory extends PointCutInvocationHandler {
    public abstract Tracer getTracer(Transaction paramTransaction, ClassMethodSignature paramClassMethodSignature,
                                     Object paramObject, Object[] paramArrayOfObject);
}