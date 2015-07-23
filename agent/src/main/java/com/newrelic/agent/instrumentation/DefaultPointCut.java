package com.newrelic.agent.instrumentation;

import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import com.newrelic.agent.tracers.TracerFactory;

public class DefaultPointCut extends PointCut {
    private final TracerFactory tracerFactory;

    public DefaultPointCut(PointCutConfiguration config, TracerFactory tracerFactory, ClassMatcher classMatcher,
                           MethodMatcher methodMatcher) {
        super(config, classMatcher, methodMatcher);
        this.tracerFactory = tracerFactory;
    }

    protected PointCutInvocationHandler getPointCutInvocationHandlerImpl() {
        return tracerFactory;
    }

    public String toString() {
        return "DefaultPointCut:" + getClass().getName();
    }
}