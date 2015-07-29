package com.newrelic.agent.jmx.create;

import java.util.List;
import java.util.Map;

import com.newrelic.agent.jmx.JmxType;

public abstract interface JmxConfiguration {
    public abstract String getObjectName();

    public abstract String getRootMetricName();

    public abstract boolean getEnabled();

    public abstract Map<JmxType, List<String>> getAttrs();
}