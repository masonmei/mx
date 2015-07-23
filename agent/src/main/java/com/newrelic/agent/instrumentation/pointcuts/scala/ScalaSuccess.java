package com.newrelic.agent.instrumentation.pointcuts.scala;

import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName = {"scala/util/Success"})
public abstract interface ScalaSuccess extends ScalaTry {
    public static final String CLASS = "scala/util/Success";

    @FieldAccessor(fieldName = "value", existingField = true)
    public abstract Object _nr_value();
}