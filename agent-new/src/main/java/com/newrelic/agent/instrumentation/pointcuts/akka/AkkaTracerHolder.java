package com.newrelic.agent.instrumentation.pointcuts.akka;

import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName={"akka/dispatch/AbstractPromise"})
public abstract interface AkkaTracerHolder
{
  @FieldAccessor(fieldName="tracer", volatileAccess=true)
  public abstract Object _nr_getTracer();

  @FieldAccessor(fieldName="tracer", volatileAccess=true)
  public abstract void _nr_setTracer(Object paramObject);
}