package com.newrelic.agent.stats;

public abstract interface ApdexStats extends StatsBase {
    public abstract void recordApdexFrustrated();

    public abstract void recordApdexResponseTime(long paramLong1, long paramLong2);

    public abstract int getApdexSatisfying();

    public abstract int getApdexTolerating();

    public abstract int getApdexFrustrating();
}