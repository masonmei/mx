package com.newrelic.agent.profile;

import java.util.concurrent.atomic.AtomicLong;

public class XrayClockTimeController extends AbstractController
{
  private long startTimeInNanos;
  private final AtomicLong runTime = new AtomicLong();

  public XrayClockTimeController(ProfilingTask profilingTask) {
    super(profilingTask);
  }

  public void run()
  {
    long startTime = System.nanoTime();
    super.run();
    this.runTime.addAndGet(System.nanoTime() - startTime);
  }

  protected int doCalculateSamplePeriodInMillis()
  {
    long runTimeInNanos = getAndResetRunTimeInNanos();
    long endTimeInNanos = getTimeInNanos();
    int samplePeriod = getSamplePeriodInMillis();
    if (this.startTimeInNanos > 0L) {
      long timeInNanos = endTimeInNanos - this.startTimeInNanos;
      samplePeriod = calculateSamplePeriodInMillis(timeInNanos, runTimeInNanos);
    }
    this.startTimeInNanos = endTimeInNanos;
    return samplePeriod;
  }

  private int calculateSamplePeriodInMillis(long timeInNanos, long runTimeInNanos) {
    if ((runTimeInNanos == 0L) || (timeInNanos == 0L)) {
      return getSamplePeriodInMillis();
    }

    float runUtilization = (float)runTimeInNanos / (float)(timeInNanos * getProcessorCount());

    return (int)(runUtilization * getSamplePeriodInMillis() / TARGET_UTILIZATION);
  }

  protected long getTimeInNanos() {
    return System.nanoTime();
  }

  protected long getAndResetRunTimeInNanos() {
    return this.runTime.getAndSet(0L);
  }
}