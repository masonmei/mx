package com.newrelic.agent.bridge;

public interface TracedMethod extends com.newrelic.api.agent.TracedMethod {
    TracedMethod getParentTracedMethod();

    void setRollupMetricNames(String[] paramArrayOfString);

    void setMetricNameFormatInfo(String paramString1, String paramString2, String paramString3);

    void addExclusiveRollupMetricName(String[] paramArrayOfString);

    void nameTransaction(TransactionNamePriority paramTransactionNamePriority);
}