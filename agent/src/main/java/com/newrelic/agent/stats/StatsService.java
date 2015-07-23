package com.newrelic.agent.stats;

import com.newrelic.agent.service.Service;
import com.newrelic.api.agent.MetricAggregator;

public interface StatsService extends Service {
    void doStatsWork(StatsWork paramStatsWork);

    StatsEngine getStatsEngineForHarvest(String paramString);

    MetricAggregator getMetricAggregator();
}