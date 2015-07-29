package com.newrelic.agent.samplers;

import com.newrelic.agent.Agent;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.stats.Stats;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.util.TimeConversion;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.text.MessageFormat;
import java.util.logging.Level;

public abstract class AbstractCPUSampler
{
  private double lastCPUTimeSeconds;
  private long lastTimestampNanos;
  private final int processorCount;

  protected AbstractCPUSampler()
  {
    this.processorCount = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
    Agent.LOG.finer(this.processorCount + " processor(s)");
  }

  protected abstract double getProcessCpuTime();

  protected void recordCPU(StatsEngine statsEngine)
  {
    double currentProcessTime = getProcessCpuTime();
    double dCPU = currentProcessTime - this.lastCPUTimeSeconds;
    this.lastCPUTimeSeconds = currentProcessTime;

    long now = System.nanoTime();
    long elapsedNanos = now - this.lastTimestampNanos;
    this.lastTimestampNanos = now;

    double elapsedTime = TimeConversion.convertNanosToSeconds(elapsedNanos);
    double utilization = dCPU / (elapsedTime * this.processorCount);

    boolean shouldLog = Agent.LOG.isLoggable(Level.FINER);
    if (shouldLog) {
      String msg = MessageFormat.format("Recorded CPU time: {0} ({1}) {2}", new Object[] { Double.valueOf(dCPU), Double.valueOf(utilization), getClass().getName() });

      Agent.LOG.finer(msg);
    }
    if ((this.lastCPUTimeSeconds > 0.0D) && (dCPU >= 0.0D)) {
      if ((Double.isNaN(dCPU)) || (Double.isInfinite(dCPU))) {
        if (shouldLog) {
          String msg = MessageFormat.format("Infinite or non-number CPU time: {0} (current) - {1} (last)", new Object[] { Double.valueOf(currentProcessTime), Double.valueOf(this.lastCPUTimeSeconds) });

          Agent.LOG.finer(msg);
        }
      }
      else statsEngine.getStats("CPU/User Time").recordDataPoint((float)dCPU);

      if ((Double.isNaN(utilization)) || (Double.isInfinite(utilization))) {
        if (shouldLog) {
          String msg = MessageFormat.format("Infinite or non-number CPU utilization: {0} ({1})", new Object[] { Double.valueOf(utilization), Double.valueOf(dCPU) });

          Agent.LOG.finer(msg);
        }
      }
      else statsEngine.getStats("CPU/User/Utilization").recordDataPoint((float)utilization);

    }
    else if (shouldLog) {
      String msg = MessageFormat.format("Bad CPU time: {0} (current) - {1} (last)", new Object[] { Double.valueOf(currentProcessTime), Double.valueOf(this.lastCPUTimeSeconds) });

      Agent.LOG.finer(msg);
    }
  }
}