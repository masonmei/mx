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
        newStats.count = count;
        newStats.total = total;
        newStats.totalExclusive = totalExclusive;
        newStats.minValue = minValue;
        newStats.maxValue = maxValue;
        newStats.sumOfSquares = sumOfSquares;
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
        sumOfSquares += responseTimeAsDouble;
        if (count > 0) {
            minValue = Math.min(responseTime, minValue);
        } else {
            minValue = responseTime;
        }
        count += 1;
        total += responseTime;
        maxValue = Math.max(responseTime, maxValue);
        totalExclusive += exclusiveTime;
    }

    public boolean hasData() {
        return (count > 0) || (total > 0L) || (totalExclusive > 0L);
    }

    public void reset() {
        count = 0;
        total = (this.totalExclusive = this.minValue = this.maxValue = 0L);
        sumOfSquares = 0.0D;
    }

    public float getTotal() {
        return (float) total / 1.0E+09F;
    }

    public float getTotalExclusiveTime() {
        return (float) totalExclusive / 1.0E+09F;
    }

    public float getMaxCallTime() {
        return (float) maxValue / 1.0E+09F;
    }

    public float getMinCallTime() {
        return (float) minValue / 1.0E+09F;
    }

    public double getSumOfSquares() {
        return sumOfSquares / 1.0E+18D;
    }

    public final void merge(StatsBase statsObj) {
        if ((statsObj instanceof ResponseTimeStatsImpl)) {
            ResponseTimeStatsImpl stats = (ResponseTimeStatsImpl) statsObj;
            if (stats.count > 0) {
                if (count > 0) {
                    minValue = Math.min(minValue, stats.minValue);
                } else {
                    minValue = stats.minValue;
                }
            }
            count += stats.count;
            total += stats.total;
            totalExclusive += stats.totalExclusive;

            maxValue = Math.max(maxValue, stats.maxValue);
            sumOfSquares += stats.sumOfSquares;
        }
    }

    public void recordResponseTime(int count, long totalTime, long minTime, long maxTime, TimeUnit unit) {
        long totalTimeInNanos = TimeUnit.NANOSECONDS.convert(totalTime, unit);
        this.count = count;
        total = totalTimeInNanos;
        totalExclusive = totalTimeInNanos;
        minValue = TimeUnit.NANOSECONDS.convert(minTime, unit);
        maxValue = TimeUnit.NANOSECONDS.convert(maxTime, unit);
        double totalTimeInNanosAsDouble = totalTimeInNanos;
        totalTimeInNanosAsDouble *= totalTimeInNanosAsDouble;
        sumOfSquares += totalTimeInNanosAsDouble;
    }

    public String toString() {
        return "ResponseTimeStatsImpl [total=" + total + ", totalExclusive=" + totalExclusive + ", minValue=" + minValue
                       + ", maxValue=" + maxValue + ", sumOfSquares=" + sumOfSquares + "]";
    }
}