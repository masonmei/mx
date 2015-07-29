package com.newrelic.agent.instrumentation;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.circuitbreaker.CircuitBreakerService;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFactory;

public abstract class TracerFactoryPointCut extends PointCut
  implements TracerFactory
{
  public TracerFactoryPointCut(Class<? extends TracerFactoryPointCut> pointCutClass, ClassMatcher classMatcher, MethodMatcher methodMatcher)
  {
    super(new PointCutConfiguration(pointCutClass), classMatcher, methodMatcher);
  }

  public TracerFactoryPointCut(PointCutConfiguration config, ClassMatcher classMatcher, MethodMatcher methodMatcher) {
    super(config, classMatcher, methodMatcher);
  }

  protected PointCutInvocationHandler getPointCutInvocationHandlerImpl()
  {
    return this;
  }

  public Tracer getTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args)
  {
    return canCreateTracer() ? doGetTracer(transaction, sig, object, args) : null;
  }

  protected abstract Tracer doGetTracer(Transaction paramTransaction, ClassMethodSignature paramClassMethodSignature, Object paramObject, Object[] paramArrayOfObject);

  public boolean canCreateTracer()
  {
    return !ServiceFactory.getServiceManager().getCircuitBreakerService().isTripped();
  }
}