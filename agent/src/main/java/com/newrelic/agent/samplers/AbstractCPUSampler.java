package com.newrelic.agent.samplers;

import java.lang.management.ManagementFactory;
import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.util.TimeConversion;

public abstract class AbstractCPUSampler {
    private final int processorCount;
    private double lastCPUTimeSeconds;
    private long lastTimestampNanos;

    protected AbstractCPUSampler() {
        processorCount = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
        Agent.LOG.finer(processorCount + " processor(s)");
    }

    protected abstract double getProcessCpuTime();

    protected void recordCPU(StatsEngine statsEngine) {
        double currentProcessTime = getProcessCpuTime();
        double dCPU = currentProcessTime - lastCPUTimeSeconds;
        lastCPUTimeSeconds = currentProcessTime;

        long now = System.nanoTime();
        long elapsedNanos = now - lastTimestampNanos;
        lastTimestampNanos = now;

        double elapsedTime = TimeConversion.convertNanosToSeconds(elapsedNanos);
        double utilization = dCPU / (elapsedTime * processorCount);

        boolean shouldLog = Agent.LOG.isLoggable(Level.FINER);
        if (shouldLog) {
            String msg = MessageFormat.format("Recorded CPU time: {0} ({1}) {2}",
                                                     new Object[] {Double.valueOf(dCPU), Double.valueOf(utilization),
                                                                          getClass().getName()});

            Agent.LOG.finer(msg);
        }
        if ((lastCPUTimeSeconds > 0.0D) && (dCPU >= 0.0D)) {
            if ((Double.isNaN(dCPU)) || (Double.isInfinite(dCPU))) {
                if (shouldLog) {
                    String msg = MessageFormat.format("Infinite or non-number CPU time: {0} (current) - {1} (last)",
                                                             new Object[] {Double.valueOf(currentProcessTime),
                                                                                  Double.valueOf(lastCPUTimeSeconds)});

                    Agent.LOG.finer(msg);
                }
            } else {
                statsEngine.getStats("CPU/User Time").recordDataPoint((float) dCPU);
            }

            if ((Double.isNaN(utilization)) || (Double.isInfinite(utilization))) {
                if (shouldLog) {
                    String msg = MessageFormat.format("Infinite or non-number CPU utilization: {0} ({1})",
                                                             new Object[] {Double.valueOf(utilization),
                                                                                  Double.valueOf(dCPU)});

                    Agent.LOG.finer(msg);
                }
            } else {
                statsEngine.getStats("CPU/User/Utilization").recordDataPoint((float) utilization);
            }

        } else if (shouldLog) {
            String msg = MessageFormat.format("Bad CPU time: {0} (current) - {1} (last)",
                                                     new Object[] {Double.valueOf(currentProcessTime),
                                                                          Double.valueOf(lastCPUTimeSeconds)});

            Agent.LOG.finer(msg);
        }
    }
}