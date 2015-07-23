package com.newrelic.agent.transaction;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TransactionTimer {
    private final long startTimeNs;
    private final AtomicLong totalTimeNs;
    private volatile long responseTimeNs;
    private long endTimeNs;

    public TransactionTimer(long startTimeNs) {
        this.startTimeNs = startTimeNs;
        totalTimeNs = new AtomicLong(0L);
    }

    public void setTransactionEndTimeIfLonger(long newEndTime) {
        if (newEndTime > endTimeNs) {
            endTimeNs = newEndTime;
            responseTimeNs = (endTimeNs - startTimeNs);
        }
    }

    public void incrementTransactionTotalTime(long rootTracerTimeNs) {
        totalTimeNs.addAndGet(rootTracerTimeNs);
    }

    public long getResponseTime() {
        return responseTimeNs;
    }

    public long getRunningDurationInNanos() {
        return responseTimeNs > 0L ? responseTimeNs : Math.max(0L, System.nanoTime() - getStartTime());
    }

    public long getTotalTime() {
        return totalTimeNs.longValue();
    }

    public long getStartTime() {
        return startTimeNs;
    }

    public long getEndTime() {
        return endTimeNs;
    }

    public long getStartTimeInMilliseconds() {
        return TimeUnit.MILLISECONDS.convert(getStartTime(), TimeUnit.NANOSECONDS);
    }

    public long getResponseTimeInMilliseconds() {
        return TimeUnit.MILLISECONDS.convert(getResponseTime(), TimeUnit.NANOSECONDS);
    }

    public long getTotalTimeInMilliseconds() {
        return TimeUnit.MILLISECONDS.convert(getTotalTime(), TimeUnit.NANOSECONDS);
    }

    public long getTEndTimeInMilliseconds() {
        return TimeUnit.MILLISECONDS.convert(getEndTime(), TimeUnit.NANOSECONDS);
    }
}