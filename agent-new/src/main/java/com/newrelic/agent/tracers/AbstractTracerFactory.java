package com.newrelic.agent.tracers;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.circuitbreaker.CircuitBreakerService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;

public abstract class AbstractTracerFactory
  implements TracerFactory
{
  public Tracer getTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args)
  {
    return canCreateTracer() ? doGetTracer(transaction, sig, object, args) : null;
  }

  public abstract Tracer doGetTracer(Transaction paramTransaction, ClassMethodSignature paramClassMethodSignature, Object paramObject, Object[] paramArrayOfObject);

  public boolean canCreateTracer() {
    return !ServiceFactory.getServiceManager().getCircuitBreakerService().isTripped();
  }
}