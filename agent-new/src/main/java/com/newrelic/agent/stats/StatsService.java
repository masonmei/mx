package com.newrelic.agent.stats;

import com.newrelic.agent.service.Service;
import com.newrelic.api.agent.MetricAggregator;

public abstract interface StatsService extends Service {
    public abstract void doStatsWork(StatsWork paramStatsWork);

    public abstract StatsEngine getStatsEngineForHarvest(String paramString);

    public abstract MetricAggregator getMetricAggregator();
}