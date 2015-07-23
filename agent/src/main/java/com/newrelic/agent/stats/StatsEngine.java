package com.newrelic.agent.stats;

import java.util.List;

import com.newrelic.agent.MetricData;
import com.newrelic.agent.metric.MetricIdRegistry;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.normalization.Normalizer;

public abstract interface StatsEngine {
    public abstract Stats getStats(String paramString);

    public abstract Stats getStats(MetricName paramMetricName);

    public abstract void recordEmptyStats(String paramString);

    public abstract void recordEmptyStats(MetricName paramMetricName);

    public abstract ResponseTimeStats getResponseTimeStats(String paramString);

    public abstract ResponseTimeStats getResponseTimeStats(MetricName paramMetricName);

    public abstract ApdexStats getApdexStats(MetricName paramMetricName);

    public abstract List<MetricName> getMetricNames();

    public abstract void clear();

    public abstract List<MetricData> getMetricData(Normalizer paramNormalizer, MetricIdRegistry paramMetricIdRegistry);

    public abstract void mergeStats(StatsEngine paramStatsEngine);

    public abstract void mergeStatsResolvingScope(TransactionStats paramTransactionStats, String paramString);

    public abstract int getSize();
}