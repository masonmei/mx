package com.newrelic.agent.config;

public abstract interface KeyTransactionConfig {
    public abstract boolean isApdexTSet(String paramString);

    public abstract long getApdexTInMillis(String paramString);
}