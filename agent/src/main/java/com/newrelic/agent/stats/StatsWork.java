package com.newrelic.agent.stats;

public interface StatsWork {
    void doWork(StatsEngine paramStatsEngine);

    String getAppName();
}