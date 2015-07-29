//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.metric;

import java.util.HashMap;
import java.util.Map;

public class MetricIdRegistry {
    public static final int METRIC_LIMIT;
    private static final int INITIAL_CAPACITY = 1000;

    static {
        String property = System.getProperty("newrelic.metric_registry_limit");
        METRIC_LIMIT = null != property ? Integer.parseInt(property) : 15000;
    }

    private final Map<MetricName, Integer> metricIds = new HashMap<MetricName, Integer>(INITIAL_CAPACITY);

    public MetricIdRegistry() {
    }

    public Integer getMetricId(MetricName metricName) {
        return this.metricIds.get(metricName);
    }

    public void setMetricId(MetricName metricName, Integer metricId) {
        if (this.metricIds.size() == METRIC_LIMIT) {
            this.metricIds.clear();
        }

        this.metricIds.put(metricName, metricId);
    }

    public void clear() {
        this.metricIds.clear();
    }

    public int getSize() {
        return this.metricIds.size();
    }
}
