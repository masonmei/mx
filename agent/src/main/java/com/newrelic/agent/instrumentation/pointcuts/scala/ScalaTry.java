package com.newrelic.agent.instrumentation.pointcuts.scala;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName = {"scala/util/Try"})
public  interface ScalaTry {
    public static final String CLASS = "scala/util/Try";
}