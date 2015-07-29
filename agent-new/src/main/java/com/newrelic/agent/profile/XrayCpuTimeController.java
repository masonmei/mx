package com.newrelic.agent.profile;

import java.lang.management.ManagementFactory;
import java.text.MessageFormat;

import com.newrelic.agent.Agent;

public class XrayCpuTimeController extends AbstractController implements ProfilingTaskController {
    private long threadId;
    private long startCpuTimeInNanos;
    private long startTimeInNanos;
    private volatile long currentThreadId = Thread.currentThread().getId();

    public XrayCpuTimeController(ProfilingTask profilingTask) {
        super(profilingTask);
    }

    public void run() {
        this.currentThreadId = Thread.currentThread().getId();
        super.run();
    }

    protected int doCalculateSamplePeriodInMillis() {
        int samplePeriod = getSamplePeriodInMillis();
        long nThreadId = getCurrentThreadId();
        long oThreadId = this.threadId;
        this.threadId = nThreadId;
        long endCpuTimeInNanos;
        try {
            endCpuTimeInNanos = getThreadCpuTimeInNanos();
        } catch (Throwable t) {
            Agent.LOG.fine(MessageFormat.format("Error getting thread cpu time: {0}", new Object[] {t}));
            return samplePeriod;
        }
        long endTimeInNanos = getTimeInNanos();
        if (oThreadId == nThreadId) {
            samplePeriod = calculateSamplePeriod(endTimeInNanos - this.startTimeInNanos,
                                                        endCpuTimeInNanos - this.startCpuTimeInNanos);
        }

        this.startCpuTimeInNanos = endCpuTimeInNanos;
        this.startTimeInNanos = endTimeInNanos;
        return samplePeriod;
    }

    private int calculateSamplePeriod(long timeInNanos, long cpuTimeInNanos) {
        if ((cpuTimeInNanos == 0L) || (timeInNanos == 0L)) {
            return getSamplePeriodInMillis();
        }

        float cpuUtilization = (float) cpuTimeInNanos / (float) (timeInNanos * getProcessorCount());

        return (int) (cpuUtilization * getSamplePeriodInMillis() / TARGET_UTILIZATION);
    }

    protected long getThreadCpuTimeInNanos() {
        return ManagementFactory.getThreadMXBean().getThreadCpuTime(this.threadId);
    }

    protected long getCurrentThreadId() {
        return this.currentThreadId;
    }

    protected long getTimeInNanos() {
        return System.nanoTime();
    }
}