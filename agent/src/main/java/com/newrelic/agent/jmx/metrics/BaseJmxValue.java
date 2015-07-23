package com.newrelic.agent.jmx.metrics;

import java.util.Arrays;
import java.util.List;

import com.newrelic.agent.jmx.create.JmxAttributeFilter;
import com.newrelic.agent.jmx.create.JmxMetricModifier;

public class BaseJmxValue {
    private final String objectNameString;
    private final String objectMetricName;
    private final List<JmxMetric> metrics;
    private final JmxAttributeFilter attributeFilter;
    private final JMXMetricType type;
    private final JmxMetricModifier modifier;

    public BaseJmxValue(String pObjectName, String pObjectMetricName, JmxMetric[] pMetrics) {
        this(pObjectName, pObjectMetricName, null, null, JMXMetricType.INCREMENT_COUNT_PER_BEAN, pMetrics);
    }

    public BaseJmxValue(String pObjectName, String pObjectMetricName, JmxAttributeFilter attributeFilter,
                        JmxMetric[] pMetrics) {
        this(pObjectName, pObjectMetricName, attributeFilter, null, JMXMetricType.INCREMENT_COUNT_PER_BEAN, pMetrics);
    }

    public BaseJmxValue(String pObjectName, String pObjectMetricName, JmxMetricModifier pModifier,
                        JmxMetric[] pMetrics) {
        this(pObjectName, pObjectMetricName, null, pModifier, JMXMetricType.INCREMENT_COUNT_PER_BEAN, pMetrics);
    }

    public BaseJmxValue(String pObjectName, String pObjectMetricName, JmxAttributeFilter attributeFilter,
                        JmxMetricModifier pModifier, JMXMetricType pType, JmxMetric[] pMetrics) {
        objectNameString = pObjectName;

        objectMetricName = pObjectMetricName;

        metrics = Arrays.asList(pMetrics);
        this.attributeFilter = attributeFilter;
        type = pType;
        modifier = pModifier;
    }

    public String getObjectNameString() {
        return objectNameString;
    }

    public String getObjectMetricName() {
        return objectMetricName;
    }

    public List<JmxMetric> getMetrics() {
        return metrics;
    }

    public JmxAttributeFilter getAttributeFilter() {
        return attributeFilter;
    }

    public JMXMetricType getType() {
        return type;
    }

    public JmxMetricModifier getModifier() {
        return modifier;
    }
}