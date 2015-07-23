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
        currentThreadId = Thread.currentThread().getId();
        super.run();
    }

    protected int doCalculateSamplePeriodInMillis() {
        int samplePeriod = getSamplePeriodInMillis();
        long nThreadId = getCurrentThreadId();
        long oThreadId = threadId;
        threadId = nThreadId;
        long endCpuTimeInNanos;
        try {
            endCpuTimeInNanos = getThreadCpuTimeInNanos();
        } catch (Throwable t) {
            Agent.LOG.fine(MessageFormat.format("Error getting thread cpu time: {0}", new Object[] {t}));
            return samplePeriod;
        }
        long endTimeInNanos = getTimeInNanos();
        if (oThreadId == nThreadId) {
            samplePeriod =
                    calculateSamplePeriod(endTimeInNanos - startTimeInNanos, endCpuTimeInNanos - startCpuTimeInNanos);
        }

        startCpuTimeInNanos = endCpuTimeInNanos;
        startTimeInNanos = endTimeInNanos;
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
        return ManagementFactory.getThreadMXBean().getThreadCpuTime(threadId);
    }

    protected long getCurrentThreadId() {
        return currentThreadId;
    }

    protected long getTimeInNanos() {
        return System.nanoTime();
    }
}