package com.newrelic.agent.tracers.metricname;

import java.text.MessageFormat;

import com.newrelic.agent.tracers.ClassMethodSignature;

public class DefaultMetricNameFormat extends AbstractMetricNameFormat {
    private final String metricName;

    public DefaultMetricNameFormat(ClassMethodSignature sig, Object object, String pattern) {
        this.metricName =
                MessageFormat.format(pattern, new Object[] {object.getClass().getName(), sig.getMethodName()});
    }

    public String getMetricName() {
        return this.metricName;
    }
}