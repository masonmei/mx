package com.newrelic.agent.stats;

import java.util.concurrent.TimeUnit;

public class ResponseTimeStatsImpl extends AbstractStats implements ResponseTimeStats {
    private static final long NANOSECONDS_PER_SECOND_SQUARED = 1000000000000000000L;
    private long total;
    private long totalExclusive;
    private long minValue;
    private long maxValue;
    private double sumOfSquares;

    public Object clone() throws CloneNotSupportedException {
        ResponseTimeStatsImpl newStats = new ResponseTimeStatsImpl();
        newStats.count = this.count;
        newStats.total = this.total;
        newStats.totalExclusive = this.totalExclusive;
        newStats.minValue = this.minValue;
        newStats.maxValue = this.maxValue;
        newStats.sumOfSquares = this.sumOfSquares;
        return newStats;
    }

    public void recordResponseTime(long responseTime, TimeUnit timeUnit) {
        long responseTimeInNanos = TimeUnit.NANOSECONDS.convert(responseTime, timeUnit);
        recordResponseTimeInNanos(responseTimeInNanos, responseTimeInNanos);
    }

    public void recordResponseTime(long responseTime, long exclusiveTime, TimeUnit timeUnit) {
        long responseTimeInNanos = TimeUnit.NANOSECONDS.convert(responseTime, timeUnit);
        long exclusiveTimeInNanos = TimeUnit.NANOSECONDS.convert(exclusiveTime, timeUnit);
        recordResponseTimeInNanos(responseTimeInNanos, exclusiveTimeInNanos);
    }

    public void recordResponseTimeInNanos(long responseTime) {
        recordResponseTimeInNanos(responseTime, responseTime);
    }

    public void recordResponseTimeInNanos(long responseTime, long exclusiveTime) {
        double responseTimeAsDouble = responseTime;
        responseTimeAsDouble *= responseTimeAsDouble;
        this.sumOfSquares += responseTimeAsDouble;
        if (this.count > 0) {
            this.minValue = Math.min(responseTime, this.minValue);
        } else {
            this.minValue = responseTime;
        }
        this.count += 1;
        this.total += responseTime;
        this.maxValue = Math.max(responseTime, this.maxValue);
        this.totalExclusive += exclusiveTime;
    }

    public boolean hasData() {
        return (this.count > 0) || (this.total > 0L) || (this.totalExclusive > 0L);
    }

    public void reset() {
        this.count = 0;
        this.total = (this.totalExclusive = this.minValue = this.maxValue = 0L);
        this.sumOfSquares = 0.0D;
    }

    public float getTotal() {
        return (float) this.total / 1.0E+09F;
    }

    public float getTotalExclusiveTime() {
        return (float) this.totalExclusive / 1.0E+09F;
    }

    public float getMaxCallTime() {
        return (float) this.maxValue / 1.0E+09F;
    }

    public float getMinCallTime() {
        return (float) this.minValue / 1.0E+09F;
    }

    public double getSumOfSquares() {
        return this.sumOfSquares / 1.0E+18D;
    }

    public final void merge(StatsBase statsObj) {
        if ((statsObj instanceof ResponseTimeStatsImpl)) {
            ResponseTimeStatsImpl stats = (ResponseTimeStatsImpl) statsObj;
            if (stats.count > 0) {
                if (this.count > 0) {
                    this.minValue = Math.min(this.minValue, stats.minValue);
                } else {
                    this.minValue = stats.minValue;
                }
            }
            this.count += stats.count;
            this.total += stats.total;
            this.totalExclusive += stats.totalExclusive;

            this.maxValue = Math.max(this.maxValue, stats.maxValue);
            this.sumOfSquares += stats.sumOfSquares;
        }
    }

    public void recordResponseTime(int count, long totalTime, long minTime, long maxTime, TimeUnit unit) {
        long totalTimeInNanos = TimeUnit.NANOSECONDS.convert(totalTime, unit);
        this.count = count;
        this.total = totalTimeInNanos;
        this.totalExclusive = totalTimeInNanos;
        this.minValue = TimeUnit.NANOSECONDS.convert(minTime, unit);
        this.maxValue = TimeUnit.NANOSECONDS.convert(maxTime, unit);
        double totalTimeInNanosAsDouble = totalTimeInNanos;
        totalTimeInNanosAsDouble *= totalTimeInNanosAsDouble;
        this.sumOfSquares += totalTimeInNanosAsDouble;
    }

    public String toString() {
        return "ResponseTimeStatsImpl [total=" + this.total + ", totalExclusive=" + this.totalExclusive + ", minValue="
                       + this.minValue + ", maxValue=" + this.maxValue + ", sumOfSquares=" + this.sumOfSquares + "]";
    }
}