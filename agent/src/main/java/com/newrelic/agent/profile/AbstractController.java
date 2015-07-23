package com.newrelic.agent.profile;

import java.lang.management.ManagementFactory;

import com.newrelic.agent.stats.StatsEngine;

public abstract class AbstractController implements ProfilingTaskController {
    static int MAX_SAMPLE_PERIOD_IN_MILLIS = 6400;
    static int MIN_SAMPLE_PERIOD_IN_MILLIS = 100;
    static float TARGET_UTILIZATION = 0.02F;
    private final ProfilingTask delegate;
    private final int processorCount;
    private int samplePeriodInMillis = -1;

    public AbstractController(ProfilingTask delegate) {
        this.delegate = delegate;
        processorCount = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
    }

    protected int getProcessorCount() {
        return processorCount;
    }

    abstract int doCalculateSamplePeriodInMillis();

    public int getSamplePeriodInMillis() {
        if (samplePeriodInMillis == -1) {
            return MIN_SAMPLE_PERIOD_IN_MILLIS;
        }
        return samplePeriodInMillis;
    }

    private void calculateSamplePeriodInMillis() {
        if (samplePeriodInMillis == -1) {
            return;
        }
        int nSamplePeriodInMillis = doCalculateSamplePeriodInMillis();
        if (nSamplePeriodInMillis > samplePeriodInMillis) {
            nSamplePeriodInMillis = samplePeriodInMillis * 2;
        } else if (nSamplePeriodInMillis <= samplePeriodInMillis / 4) {
            nSamplePeriodInMillis = samplePeriodInMillis / 2;
        } else {
            nSamplePeriodInMillis = samplePeriodInMillis;
        }

        nSamplePeriodInMillis =
                Math.min(MAX_SAMPLE_PERIOD_IN_MILLIS, Math.max(nSamplePeriodInMillis, MIN_SAMPLE_PERIOD_IN_MILLIS));

        samplePeriodInMillis = nSamplePeriodInMillis;
    }

    public void run() {
        delegate.run();
    }

    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        delegate.beforeHarvest(appName, statsEngine);
    }

    public void afterHarvest(String appName) {
        calculateSamplePeriodInMillis();
        delegate.afterHarvest(appName);
    }

    public void addProfile(ProfilerParameters parameters) {
        if (samplePeriodInMillis == -1) {
            samplePeriodInMillis = parameters.getSamplePeriodInMillis().intValue();
        }
        delegate.addProfile(parameters);
    }

    public void removeProfile(ProfilerParameters parameters) {
        delegate.removeProfile(parameters);
    }

    ProfilingTask getDelegate() {
        return delegate;
    }
}