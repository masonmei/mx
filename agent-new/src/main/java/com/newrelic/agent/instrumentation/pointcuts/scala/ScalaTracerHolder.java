package com.newrelic.agent.instrumentation.pointcuts.scala;

import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName = {"scala/concurrent/impl/AbstractPromise"})
public abstract interface ScalaTracerHolder {
    @FieldAccessor(fieldName = "tracer", volatileAccess = true)
    public abstract Object _nr_getTracer();

    @FieldAccessor(fieldName = "tracer", volatileAccess = true)
    public abstract void _nr_setTracer(Object paramObject);
}