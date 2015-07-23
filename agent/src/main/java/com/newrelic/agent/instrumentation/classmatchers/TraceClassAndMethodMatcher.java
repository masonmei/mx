package com.newrelic.agent.instrumentation.classmatchers;

import com.newrelic.agent.instrumentation.tracing.TraceDetails;

public abstract interface TraceClassAndMethodMatcher extends ClassAndMethodMatcher {
    public abstract TraceDetails getTraceDetails();
}