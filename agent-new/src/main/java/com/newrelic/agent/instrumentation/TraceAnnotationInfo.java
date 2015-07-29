package com.newrelic.agent.instrumentation;

public class TraceAnnotationInfo {
    boolean dispatcher;
    String metricName;
    String tracerFactoryName;
    boolean skipTransactionTrace;

    public boolean dispatcher() {
        return this.dispatcher;
    }

    public String metricName() {
        return this.metricName;
    }

    public String tracerFactoryName() {
        return this.tracerFactoryName;
    }

    public boolean skipTransactionTrace() {
        return this.skipTransactionTrace;
    }
}