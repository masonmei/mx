package com.newrelic.agent.instrumentation.context;

import org.objectweb.asm.commons.Method;

import com.newrelic.agent.instrumentation.tracing.TraceDetails;

public interface TraceDetailsList {
    void addTrace(Method paramMethod, TraceDetails paramTraceDetails);
}