package com.newrelic.agent.stats;

import java.util.List;

import com.newrelic.agent.MetricData;
import com.newrelic.agent.metric.MetricIdRegistry;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.normalization.Normalizer;

public interface StatsEngine {
    Stats getStats(String paramString);

    Stats getStats(MetricName paramMetricName);

    void recordEmptyStats(String paramString);

    void recordEmptyStats(MetricName paramMetricName);

    ResponseTimeStats getResponseTimeStats(String paramString);

    ResponseTimeStats getResponseTimeStats(MetricName paramMetricName);

    ApdexStats getApdexStats(MetricName paramMetricName);

    List<MetricName> getMetricNames();

    void clear();

    List<MetricData> getMetricData(Normalizer paramNormalizer, MetricIdRegistry paramMetricIdRegistry);

    void mergeStats(StatsEngine paramStatsEngine);

    void mergeStatsResolvingScope(TransactionStats paramTransactionStats, String paramString);

    int getSize();
}