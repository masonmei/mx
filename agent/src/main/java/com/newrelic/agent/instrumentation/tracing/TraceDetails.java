package com.newrelic.agent.instrumentation.tracing;

import java.util.List;

import com.newrelic.agent.instrumentation.InstrumentationType;

public abstract interface TraceDetails {
    public abstract String metricName();

    public abstract String[] rollupMetricName();

    public abstract boolean dispatcher();

    public abstract TransactionName transactionName();

    public abstract String tracerFactoryName();

    public abstract boolean excludeFromTransactionTrace();

    public abstract String metricPrefix();

    public abstract String getFullMetricName(String paramString1, String paramString2);

    public abstract boolean ignoreTransaction();

    public abstract List<InstrumentationType> instrumentationTypes();

    public abstract List<String> instrumentationSourceNames();

    public abstract boolean isCustom();

    public abstract boolean isLeaf();

    public abstract boolean isWebTransaction();

    public abstract List<ParameterAttributeName> getParameterAttributeNames();
}