package com.newrelic.agent.profile;

import com.newrelic.agent.stats.StatsEngine;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public abstract class AbstractController
  implements ProfilingTaskController
{
  static int MAX_SAMPLE_PERIOD_IN_MILLIS = 6400;
  static int MIN_SAMPLE_PERIOD_IN_MILLIS = 100;
  static float TARGET_UTILIZATION = 0.02F;
  private final ProfilingTask delegate;
  private final int processorCount;
  private int samplePeriodInMillis = -1;

  public AbstractController(ProfilingTask delegate) {
    this.delegate = delegate;
    this.processorCount = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
  }

  protected int getProcessorCount() {
    return this.processorCount;
  }

  abstract int doCalculateSamplePeriodInMillis();

  public int getSamplePeriodInMillis()
  {
    if (this.samplePeriodInMillis == -1) {
      return MIN_SAMPLE_PERIOD_IN_MILLIS;
    }
    return this.samplePeriodInMillis;
  }

  private void calculateSamplePeriodInMillis() {
    if (this.samplePeriodInMillis == -1) {
      return;
    }
    int nSamplePeriodInMillis = doCalculateSamplePeriodInMillis();
    if (nSamplePeriodInMillis > this.samplePeriodInMillis)
    {
      nSamplePeriodInMillis = this.samplePeriodInMillis * 2;
    } else if (nSamplePeriodInMillis <= this.samplePeriodInMillis / 4)
    {
      nSamplePeriodInMillis = this.samplePeriodInMillis / 2;
    }
    else nSamplePeriodInMillis = this.samplePeriodInMillis;

    nSamplePeriodInMillis = Math.min(MAX_SAMPLE_PERIOD_IN_MILLIS, Math.max(nSamplePeriodInMillis, MIN_SAMPLE_PERIOD_IN_MILLIS));

    this.samplePeriodInMillis = nSamplePeriodInMillis;
  }

  public void run()
  {
    this.delegate.run();
  }

  public void beforeHarvest(String appName, StatsEngine statsEngine)
  {
    this.delegate.beforeHarvest(appName, statsEngine);
  }

  public void afterHarvest(String appName)
  {
    calculateSamplePeriodInMillis();
    this.delegate.afterHarvest(appName);
  }

  public void addProfile(ProfilerParameters parameters)
  {
    if (this.samplePeriodInMillis == -1) {
      this.samplePeriodInMillis = parameters.getSamplePeriodInMillis().intValue();
    }
    this.delegate.addProfile(parameters);
  }

  public void removeProfile(ProfilerParameters parameters)
  {
    this.delegate.removeProfile(parameters);
  }

  ProfilingTask getDelegate()
  {
    return this.delegate;
  }
}