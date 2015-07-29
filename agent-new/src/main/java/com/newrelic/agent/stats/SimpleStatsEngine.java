package com.newrelic.agent.stats;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.newrelic.agent.MetricData;
import com.newrelic.agent.metric.MetricIdRegistry;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.normalization.Normalizer;
import com.newrelic.agent.service.ServiceFactory;

public class SimpleStatsEngine {
    public static final int DEFAULT_CAPACITY = 32;
    private static final float SCOPED_METRIC_THRESHOLD = 0.02F;
    private final Map<String, StatsBase> stats;

    public SimpleStatsEngine() {
        this(32);
    }

    public SimpleStatsEngine(int capacity) {
        this.stats = new HashMap(capacity);
    }

    protected static MetricData createMetricData(MetricName metricName, StatsBase statsBase,
                                                 Normalizer metricNormalizer, MetricIdRegistry metricIdRegistry) {
        if (!statsBase.hasData()) {
            return null;
        }
        Integer metricId = metricIdRegistry.getMetricId(metricName);
        if (metricId != null) {
            return MetricData.create(metricName, metricId, statsBase);
        }
        String normalized = metricNormalizer.normalize(metricName.getName());
        if (normalized == null) {
            return null;
        }
        if (normalized == metricName.getName()) {
            return MetricData.create(metricName, statsBase);
        }
        MetricName normalizedMetricName = MetricName.create(normalized, metricName.getScope());
        metricId = metricIdRegistry.getMetricId(normalizedMetricName);
        if (metricId == null) {
            return MetricData.create(normalizedMetricName, statsBase);
        }
        return MetricData.create(normalizedMetricName, metricId, statsBase);
    }

    public Map<String, StatsBase> getStatsMap() {
        return this.stats;
    }

    public Stats getStats(String metricName) {
        if (metricName == null) {
            throw new RuntimeException("Cannot get a stat for a null metric");
        }
        StatsBase s = (StatsBase) this.stats.get(metricName);
        if (s == null) {
            s = new StatsImpl();
            this.stats.put(metricName, s);
        }
        if ((s instanceof Stats)) {
            return (Stats) s;
        }
        String msg = MessageFormat.format("The stats object for {0} is of type {1}",
                                                 new Object[] {metricName, s.getClass().getName()});

        throw new RuntimeException(msg);
    }

    public ResponseTimeStats getResponseTimeStats(String metric) {
        if (metric == null) {
            throw new RuntimeException("Cannot get a stat for a null metric");
        }
        StatsBase s = (StatsBase) this.stats.get(metric);
        if (s == null) {
            s = new ResponseTimeStatsImpl();
            this.stats.put(metric, s);
        }
        if ((s instanceof ResponseTimeStats)) {
            return (ResponseTimeStats) s;
        }
        String msg = MessageFormat.format("The stats object for {0} is of type {1}",
                                                 new Object[] {metric, s.getClass().getName()});
        throw new RuntimeException(msg);
    }

    public void recordEmptyStats(String metricName) {
        if (metricName == null) {
            throw new RuntimeException("Cannot record a stat for a null metric");
        }
        this.stats.put(metricName, AbstractStats.EMPTY_STATS);
    }

    public ApdexStats getApdexStats(String metricName) {
        if (metricName == null) {
            throw new RuntimeException("Cannot get a stat for a null metric");
        }
        StatsBase s = (StatsBase) this.stats.get(metricName);
        if (s == null) {
            s = new ApdexStatsImpl();
            this.stats.put(metricName, s);
        }
        if ((s instanceof ApdexStats)) {
            return (ApdexStats) s;
        }
        String msg = MessageFormat.format("The stats object for {0} is of type {1}",
                                                 new Object[] {metricName, s.getClass().getName()});

        throw new RuntimeException(msg);
    }

    public void mergeStats(SimpleStatsEngine other) {
        for (Entry<String, StatsBase> entry : other.stats.entrySet()) {
            StatsBase ourStats = this.stats.get(entry.getKey());
            StatsBase otherStats = entry.getValue();
            if (ourStats == null) {
                this.stats.put(entry.getKey(), otherStats);
            } else {
                ourStats.merge(otherStats);
            }
        }
    }

    public void clear() {
        this.stats.clear();
    }

    public int getSize() {
        return this.stats.size();
    }

    public List<MetricData> getMetricData(Normalizer metricNormalizer, MetricIdRegistry metricIdRegistry,
                                          String scope) {
        List result = new ArrayList(this.stats.size() + 1);
        boolean isTrimStats = ServiceFactory.getConfigService().getDefaultAgentConfig().isTrimStats();

        if ((isTrimStats) && (scope != "")) {
            trimStats();
        }

        for (Entry entry : this.stats.entrySet()) {
            MetricName metricName = MetricName.create((String) entry.getKey(), scope);
            MetricData metricData =
                    createMetricData(metricName, (StatsBase) entry.getValue(), metricNormalizer, metricIdRegistry);
            if (metricData != null) {
                result.add(metricData);
            }
        }
        return result;
    }

    private void trimStats() {
        float totalTime = 0.0F;
        for (StatsBase statsBase : this.stats.values()) {
            ResponseTimeStats stats = (ResponseTimeStats) statsBase;
            totalTime += stats.getTotalExclusiveTime();
        }

        ResponseTimeStatsImpl other = null;
        float threshold = totalTime * 0.02F;
        Set<String> remove = new HashSet<String>();
        for (Entry<String, StatsBase> entry : stats.entrySet()) {
            ResponseTimeStatsImpl statsObj = (ResponseTimeStatsImpl) entry.getValue();
            if ((statsObj.getTotalExclusiveTime() < threshold) && (trimmableMetric((String) entry.getKey()))) {
                if (other == null) {
                    other = statsObj;
                } else {
                    other.merge(statsObj);
                }
                remove.add(entry.getKey());
            }
        }
        if (other != null) {
            this.stats.put("Java/other", other);
            for (String name : remove) {
                this.stats.remove(name);
            }
        }
    }

    private boolean trimmableMetric(String key) {
        return (!key.startsWith("Datastore")) && (!key.startsWith("External"))
                       && (!key.startsWith("RequestDispatcher"));
    }

    public String toString() {
        return "SimpleStatsEngine [stats=" + this.stats + "]";
    }
}