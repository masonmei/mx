package com.newrelic.agent.xray;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class XRaySession
{
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

  public XRaySession(Long xRayId, boolean runProfiler, String keyTransactionName, double samplePeriod, String xRaySessionName, Long durationSeconds, Long requestedTraceCount, String applicationName)
  {
    this.xRayId = xRayId;
    this.runProfiler = runProfiler;
    this.keyTransactionName = keyTransactionName;
    this.samplePeriodMillis = ((long)(samplePeriod * 1000.0D));
    this.xRaySessionName = xRaySessionName;
    this.durationMilliseconds = TimeUnit.SECONDS.toMillis(durationSeconds.longValue());
    this.requestedTraceCount = requestedTraceCount.longValue();
    this.sessionEndTimeInNanos = (System.nanoTime() + TimeUnit.SECONDS.toNanos(durationSeconds.longValue()));

    this.endTimeForDisplay = new Date(TimeUnit.NANOSECONDS.toMillis(this.sessionEndTimeInNanos)).toString();
    this.applicationName = applicationName;
  }

  public void incrementCount() {
    this.collectedTraceCount.incrementAndGet();
  }

  public Long getxRayId() {
    return this.xRayId;
  }

  public boolean isRunProfiler() {
    return this.runProfiler;
  }

  public String getKeyTransactionName() {
    return this.keyTransactionName;
  }

  public long getSamplePeriodMilliseconds() {
    return this.samplePeriodMillis;
  }

  public String getxRaySessionName() {
    return this.xRaySessionName;
  }

  public long getDurationMilliseconds() {
    return this.durationMilliseconds;
  }

  public long getRequestedTraceCount() {
    return this.requestedTraceCount;
  }

  public long getSessionEndTimeInNanos() {
    return this.sessionEndTimeInNanos;
  }

  public long getCollectedTraceCount() {
    return this.collectedTraceCount.get();
  }

  public String getApplicationName() {
    return this.applicationName;
  }

  public boolean sessionHasExpired() {
    return (this.collectedTraceCount.get() >= this.requestedTraceCount) || (System.nanoTime() > this.sessionEndTimeInNanos);
  }

  public String toString()
  {
    return String.format("XRaySession [xRayId=%s, applicationName=%s, runProfiler=%s, keyTransactionName=%s, samplePeriodMilliseconds=%s, xRaySessionName=%s, durationMilliseconds=%s, requestedTraceCount=%s, sessionEndTimeInMillis=%s, collectedTraceCount=%s, derived end time=%s]", new Object[] { this.xRayId, this.applicationName, Boolean.valueOf(this.runProfiler), this.keyTransactionName, Long.valueOf(this.samplePeriodMillis), this.xRaySessionName, Long.valueOf(this.durationMilliseconds), Long.valueOf(this.requestedTraceCount), Long.valueOf(this.sessionEndTimeInNanos), this.collectedTraceCount, this.endTimeForDisplay });
  }
}