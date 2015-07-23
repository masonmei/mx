package com.newrelic.agent.stats;

public interface CountStats extends StatsBase {
    void incrementCallCount();

    void incrementCallCount(int paramInt);

    int getCallCount();

    void setCallCount(int paramInt);

    float getTotal();

    float getTotalExclusiveTime();

    float getMinCallTime();

    float getMaxCallTime();

    double getSumOfSquares();
}