package com.newrelic.agent.instrumentation.context;

import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

public abstract interface TraceDetailsList {
    public abstract void addTrace(Method paramMethod, TraceDetails paramTraceDetails);
}