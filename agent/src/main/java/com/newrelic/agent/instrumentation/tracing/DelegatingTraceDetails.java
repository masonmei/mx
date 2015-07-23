package com.newrelic.agent.instrumentation.tracing;

import java.util.List;

import com.newrelic.agent.instrumentation.InstrumentationType;

public class DelegatingTraceDetails implements TraceDetails {
    private final TraceDetails delegate;

    public DelegatingTraceDetails(TraceDetails delegate) {
        this.delegate = delegate;
    }

    public String metricName() {
        return delegate.metricName();
    }

    public boolean dispatcher() {
        return delegate.dispatcher();
    }

    public String tracerFactoryName() {
        return delegate.tracerFactoryName();
    }

    public boolean excludeFromTransactionTrace() {
        return delegate.excludeFromTransactionTrace();
    }

    public String metricPrefix() {
        return delegate.metricPrefix();
    }

    public String getFullMetricName(String className, String methodName) {
        return delegate.getFullMetricName(className, methodName);
    }

    public boolean ignoreTransaction() {
        return delegate.ignoreTransaction();
    }

    public boolean isCustom() {
        return delegate.isCustom();
    }

    public TransactionName transactionName() {
        return delegate.transactionName();
    }

    public List<InstrumentationType> instrumentationTypes() {
        return delegate.instrumentationTypes();
    }

    public List<String> instrumentationSourceNames() {
        return delegate.instrumentationSourceNames();
    }

    public boolean isWebTransaction() {
        return delegate.isWebTransaction();
    }

    public boolean isLeaf() {
        return delegate.isLeaf();
    }

    public String[] rollupMetricName() {
        return delegate.rollupMetricName();
    }

    public List<ParameterAttributeName> getParameterAttributeNames() {
        return delegate.getParameterAttributeNames();
    }
}