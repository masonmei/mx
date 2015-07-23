package com.newrelic.agent.stats;

public interface ApdexStats extends StatsBase {
    void recordApdexFrustrated();

    void recordApdexResponseTime(long paramLong1, long paramLong2);

    int getApdexSatisfying();

    int getApdexTolerating();

    int getApdexFrustrating();
}