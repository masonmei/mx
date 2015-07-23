package com.newrelic.agent.stats;

import java.util.concurrent.TimeUnit;

public interface ResponseTimeStats extends CountStats {
    void recordResponseTime(long paramLong, TimeUnit paramTimeUnit);

    void recordResponseTime(long paramLong1, long paramLong2, TimeUnit paramTimeUnit);

    void recordResponseTime(int paramInt, long paramLong1, long paramLong2, long paramLong3, TimeUnit paramTimeUnit);

    void recordResponseTimeInNanos(long paramLong1, long paramLong2);

    void recordResponseTimeInNanos(long paramLong);
}