package com.newrelic.agent.instrumentation.pointcuts.scala;

import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName={"scala/util/Failure"})
public abstract interface ScalaFailure extends ScalaTry
{
  public static final String CLASS = "scala/util/Failure";

  @FieldAccessor(fieldName="exception", existingField=true)
  public abstract Throwable _nr_exception();
}