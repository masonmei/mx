package com.newrelic.agent.stats;

import java.util.concurrent.TimeUnit;

public abstract interface ResponseTimeStats extends CountStats {
    public abstract void recordResponseTime(long paramLong, TimeUnit paramTimeUnit);

    public abstract void recordResponseTime(long paramLong1, long paramLong2, TimeUnit paramTimeUnit);

    public abstract void recordResponseTime(int paramInt, long paramLong1, long paramLong2, long paramLong3,
                                            TimeUnit paramTimeUnit);

    public abstract void recordResponseTimeInNanos(long paramLong1, long paramLong2);

    public abstract void recordResponseTimeInNanos(long paramLong);
}