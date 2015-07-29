package com.newrelic.agent.stats;

public abstract interface StatsWork {
    public abstract void doWork(StatsEngine paramStatsEngine);

    public abstract String getAppName();
}