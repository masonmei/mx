package com.newrelic.agent.jmx.create;

public abstract interface JmxAttributeFilter {
    public abstract boolean keepMetric(String paramString);
}