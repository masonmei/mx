package com.newrelic.agent.tracers;

import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.util.Strings;

public class MetricNameFormatWithHost implements MetricNameFormat {
    private final String host;
    private final String metricName;

    private MetricNameFormatWithHost(String host, String library) {
        this.host = host;
        metricName = Strings.join('/', new String[] {"External", host, library});
    }

    public static MetricNameFormatWithHost create(String host, String library) {
        return new MetricNameFormatWithHost(host, library);
    }

    public String getHost() {
        return host;
    }

    public String getMetricName() {
        return metricName;
    }

    public String getTransactionSegmentName() {
        return metricName;
    }

    public String getTransactionSegmentUri() {
        return null;
    }
}