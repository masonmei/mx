package com.newrelic.agent.instrumentation.context;

import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

public interface TraceDetailsList {
    void addTrace(Method paramMethod, TraceDetails paramTraceDetails);
}