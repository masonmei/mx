package com.newrelic.agent.instrumentation.pointcuts.scala;

import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName = {"scala/util/Left", "scala/Left"})
public abstract interface ScalaLeft extends Either {
    public static final String CLASS = "scala/util/Left";
    public static final String CLASS2 = "scala/Left";

    @FieldAccessor(fieldName = "a", existingField = true)
    public abstract Object get();
}