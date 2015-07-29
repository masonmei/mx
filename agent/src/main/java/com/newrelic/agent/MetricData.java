package com.newrelic.agent;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.stats.StatsBase;
import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.JSONStreamAware;

public class MetricData implements JSONStreamAware {
    private final MetricName metricName;
    private final Integer metricId;
    private final StatsBase stats;

    private MetricData(MetricName metricName, Integer metricId, StatsBase stats) {
        this.stats = stats;
        this.metricId = metricId;
        this.metricName = metricName;
    }

    public static MetricData create(MetricName metricName, StatsBase stats) {
        return create(metricName, null, stats);
    }

    public static MetricData create(MetricName metricName, Integer metricId, StatsBase stats) {
        return new MetricData(metricName, metricId, stats);
    }

    public StatsBase getStats() {
        return stats;
    }

    public MetricName getMetricName() {
        return metricName;
    }

    public Integer getMetricId() {
        return metricId;
    }

    public Object getKey() {
        return metricId != null ? metricId : metricName;
    }

    public String toString() {
        return metricName.toString();
    }

    public void writeJSONString(Writer writer) throws IOException {
        List result = new ArrayList(2);
        if (metricId == null) {
            result.add(metricName);
        } else {
            result.add(metricId);
        }
        result.add(stats);
        JSONArray.writeJSONString(result, writer);
    }
}