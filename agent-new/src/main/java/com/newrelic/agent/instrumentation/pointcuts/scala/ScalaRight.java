package com.newrelic.agent.instrumentation.pointcuts.scala;

import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName = {"scala/util/Right", "scala/Right"})
public abstract interface ScalaRight extends Either {
    public static final String CLASS = "scala/util/Right";
    public static final String CLASS2 = "scala/Right";

    @FieldAccessor(fieldName = "b", existingField = true)
    public abstract Object get();
}