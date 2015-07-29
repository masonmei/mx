package com.newrelic.agent.samplers;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.security.AccessControlException;
import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.stats.StatsEngine;

public class ThreadSampler implements MetricSampler {
    private final ThreadMXBean threadMXBean;

    public ThreadSampler() {
        this.threadMXBean = ManagementFactory.getThreadMXBean();
    }

    public void sample(StatsEngine statsEngine) {
        int threadCount = this.threadMXBean.getThreadCount();
        statsEngine.getStats("Threads/all").setCallCount(threadCount);
        long[] deadlockedThreadIds;
        try {
            deadlockedThreadIds = this.threadMXBean.findMonitorDeadlockedThreads();
        } catch (AccessControlException e) {
            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = MessageFormat
                                     .format("An error occurred calling ThreadMXBean.findMonitorDeadlockedThreads: {0}",
                                                    new Object[] {e});

                Agent.LOG.warning(msg);
            }
            deadlockedThreadIds = new long[0];
        }
        int deadlockCount = deadlockedThreadIds == null ? 0 : deadlockedThreadIds.length;
        statsEngine.getStats("Threads/Deadlocks/all").setCallCount(deadlockCount);
    }
}