package com.newrelic.agent.bridge;

public final class NoOpTracedMethod implements TracedMethod {
    public static final TracedMethod INSTANCE = new NoOpTracedMethod();

    public void nameTransaction(TransactionNamePriority namePriority) {
    }

    public TracedMethod getParentTracedMethod() {
        return null;
    }

    public String getMetricName() {
        return "NoOpTracedMethod";
    }

    public void setMetricName(String[] metricNameParts) {
    }

    public void setRollupMetricNames(String[] metricNames) {
    }

    public void addRollupMetricName(String[] metricNameParts) {
    }

    public void addExclusiveRollupMetricName(String[] metricNameParts) {
    }

    public void setMetricNameFormatInfo(String metricName, String transactionSegmentName,
                                        String transactionSegmentUri) {
    }
}