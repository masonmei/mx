package com.newrelic.agent.tracers;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.service.ServiceFactory;

public abstract class AbstractTracerFactory implements TracerFactory {
    public Tracer getTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args) {
        return canCreateTracer() ? doGetTracer(transaction, sig, object, args) : null;
    }

    public abstract Tracer doGetTracer(Transaction paramTransaction, ClassMethodSignature paramClassMethodSignature,
                                       Object paramObject, Object[] paramArrayOfObject);

    public boolean canCreateTracer() {
        return !ServiceFactory.getServiceManager().getCircuitBreakerService().isTripped();
    }
}