package com.newrelic.agent.instrumentation.pointcuts.container.jetty;

import java.util.List;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName = {"org/eclipse/jetty/util/MultiException"})
public abstract interface MultiException {
    public abstract List<Throwable> getThrowables();
}