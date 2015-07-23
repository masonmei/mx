package com.newrelic.agent;

import com.newrelic.agent.stats.StatsEngine;

public interface HarvestListener {
    void beforeHarvest(String paramString, StatsEngine paramStatsEngine);

    void afterHarvest(String paramString);
}