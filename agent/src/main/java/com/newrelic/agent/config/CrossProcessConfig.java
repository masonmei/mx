package com.newrelic.agent.config;

public abstract interface CrossProcessConfig {
    public abstract String getCrossProcessId();

    public abstract String getEncodedCrossProcessId();

    public abstract String getEncodingKey();

    public abstract boolean isTrustedAccountId(String paramString);

    public abstract boolean isCrossApplicationTracing();
}