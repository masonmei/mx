package com.newrelic.agent.xray;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class XRaySession {
    private final Long xRayId;
    private final boolean runProfiler;
    private final String keyTransactionName;
    private final long samplePeriodMillis;
    private final String xRaySessionName;
    private final long durationMilliseconds;
    private final long requestedTraceCount;
    private final String applicationName;
    private final long sessionEndTimeInNanos;
    private final AtomicInteger collectedTraceCount = new AtomicInteger(0);
    private final String endTimeForDisplay;

    public XRaySession(Long xRayId, boolean runProfiler, String keyTransactionName, double samplePeriod,
                       String xRaySessionName, Long durationSeconds, Long requestedTraceCount, String applicationName) {
        this.xRayId = xRayId;
        this.runProfiler = runProfiler;
        this.keyTransactionName = keyTransactionName;
        samplePeriodMillis = ((long) (samplePeriod * 1000.0D));
        this.xRaySessionName = xRaySessionName;
        durationMilliseconds = TimeUnit.SECONDS.toMillis(durationSeconds.longValue());
        this.requestedTraceCount = requestedTraceCount.longValue();
        sessionEndTimeInNanos = (System.nanoTime() + TimeUnit.SECONDS.toNanos(durationSeconds.longValue()));

        endTimeForDisplay = new Date(TimeUnit.NANOSECONDS.toMillis(sessionEndTimeInNanos)).toString();
        this.applicationName = applicationName;
    }

    public void incrementCount() {
        collectedTraceCount.incrementAndGet();
    }

    public Long getxRayId() {
        return xRayId;
    }

    public boolean isRunProfiler() {
        return runProfiler;
    }

    public String getKeyTransactionName() {
        return keyTransactionName;
    }

    public long getSamplePeriodMilliseconds() {
        return samplePeriodMillis;
    }

    public String getxRaySessionName() {
        return xRaySessionName;
    }

    public long getDurationMilliseconds() {
        return durationMilliseconds;
    }

    public long getRequestedTraceCount() {
        return requestedTraceCount;
    }

    public long getSessionEndTimeInNanos() {
        return sessionEndTimeInNanos;
    }

    public long getCollectedTraceCount() {
        return collectedTraceCount.get();
    }

    public String getApplicationName() {
        return applicationName;
    }

    public boolean sessionHasExpired() {
        return (collectedTraceCount.get() >= requestedTraceCount) || (System.nanoTime() > sessionEndTimeInNanos);
    }

    public String toString() {
        return String.format("XRaySession [xRayId=%s, applicationName=%s, runProfiler=%s, keyTransactionName=%s, "
                                     + "samplePeriodMilliseconds=%s, xRaySessionName=%s, durationMilliseconds=%s, "
                                     + "requestedTraceCount=%s, sessionEndTimeInMillis=%s, collectedTraceCount=%s, "
                                     + "derived end time=%s]",
                                    new Object[] {xRayId, applicationName, Boolean.valueOf(runProfiler),
                                                         keyTransactionName, Long.valueOf(samplePeriodMillis),
                                                         xRaySessionName, Long.valueOf(durationMilliseconds),
                                                         Long.valueOf(requestedTraceCount),
                                                         Long.valueOf(sessionEndTimeInNanos), collectedTraceCount,
                                                         endTimeForDisplay});
    }
}