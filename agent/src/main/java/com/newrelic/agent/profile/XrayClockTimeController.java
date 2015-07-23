package com.newrelic.agent.profile;

import java.util.concurrent.atomic.AtomicLong;

public class XrayClockTimeController extends AbstractController {
    private final AtomicLong runTime = new AtomicLong();
    private long startTimeInNanos;

    public XrayClockTimeController(ProfilingTask profilingTask) {
        super(profilingTask);
    }

    public void run() {
        long startTime = System.nanoTime();
        super.run();
        runTime.addAndGet(System.nanoTime() - startTime);
    }

    protected int doCalculateSamplePeriodInMillis() {
        long runTimeInNanos = getAndResetRunTimeInNanos();
        long endTimeInNanos = getTimeInNanos();
        int samplePeriod = getSamplePeriodInMillis();
        if (startTimeInNanos > 0L) {
            long timeInNanos = endTimeInNanos - startTimeInNanos;
            samplePeriod = calculateSamplePeriodInMillis(timeInNanos, runTimeInNanos);
        }
        startTimeInNanos = endTimeInNanos;
        return samplePeriod;
    }

    private int calculateSamplePeriodInMillis(long timeInNanos, long runTimeInNanos) {
        if ((runTimeInNanos == 0L) || (timeInNanos == 0L)) {
            return getSamplePeriodInMillis();
        }

        float runUtilization = (float) runTimeInNanos / (float) (timeInNanos * getProcessorCount());

        return (int) (runUtilization * getSamplePeriodInMillis() / TARGET_UTILIZATION);
    }

    protected long getTimeInNanos() {
        return System.nanoTime();
    }

    protected long getAndResetRunTimeInNanos() {
        return runTime.getAndSet(0L);
    }
}