package com.newrelic.agent.jmx.create;

public abstract interface JmxMetricModifier {
    public abstract String getMetricName(String paramString);
}