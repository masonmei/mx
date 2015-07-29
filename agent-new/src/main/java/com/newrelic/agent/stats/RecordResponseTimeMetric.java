package com.newrelic.agent.stats;

import java.util.concurrent.TimeUnit;

public final class RecordResponseTimeMetric implements StatsWork {
    private final long totalInMillis;
    private final long exclusiveTimeInMillis;
    private final String name;
    private final TimeUnit timeUnit;

    public RecordResponseTimeMetric(long millis, String name, TimeUnit timeUnit) {
        this(millis, millis, name, timeUnit);
    }

    public RecordResponseTimeMetric(long totalInMillis, long exclusiveTimeInMillis, String name, TimeUnit timeUnit) {
        this.exclusiveTimeInMillis = exclusiveTimeInMillis;
        this.totalInMillis = totalInMillis;
        this.timeUnit = timeUnit;

        this.name = name;
    }

    public void doWork(StatsEngine statsEngine) {
        statsEngine.getResponseTimeStats(this.name)
                .recordResponseTime(this.totalInMillis, this.exclusiveTimeInMillis, this.timeUnit);
    }

    public String getAppName() {
        return null;
    }
}