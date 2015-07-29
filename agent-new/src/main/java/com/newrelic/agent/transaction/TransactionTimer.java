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
        this.totalTimeNs = new AtomicLong(0L);
    }

    public void setTransactionEndTimeIfLonger(long newEndTime) {
        if (newEndTime > this.endTimeNs) {
            this.endTimeNs = newEndTime;
            this.responseTimeNs = (this.endTimeNs - this.startTimeNs);
        }
    }

    public void incrementTransactionTotalTime(long rootTracerTimeNs) {
        this.totalTimeNs.addAndGet(rootTracerTimeNs);
    }

    public long getResponseTime() {
        return this.responseTimeNs;
    }

    public long getRunningDurationInNanos() {
        return this.responseTimeNs > 0L ? this.responseTimeNs : Math.max(0L, System.nanoTime() - getStartTime());
    }

    public long getTotalTime() {
        return this.totalTimeNs.longValue();
    }

    public long getStartTime() {
        return this.startTimeNs;
    }

    public long getEndTime() {
        return this.endTimeNs;
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