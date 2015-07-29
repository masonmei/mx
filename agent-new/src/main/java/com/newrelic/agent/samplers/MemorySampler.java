package com.newrelic.agent.samplers;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.stats.StatsEngine;

public class MemorySampler implements MetricSampler {
  public static final float BYTES_PER_MB = 1048576.0F;
  private final MemoryMXBean memoryMXBean;

  public MemorySampler() {
    memoryMXBean = ManagementFactory.getMemoryMXBean();
  }

  void start() {
  }

  public void sample(StatsEngine statsEngine) {
    sampleMemory(statsEngine);
    sampleMemoryPools(statsEngine);
  }

  private void sampleMemory(StatsEngine statsEngine) {
    try {
      HeapAndNonHeapUsage heapUsage = new HeapAndNonHeapUsage(memoryMXBean);
      heapUsage.recordStats(statsEngine);
    } catch (Exception e) {
      String msg = MessageFormat.format("An error occurred gathering memory metrics: {0}", new Object[] {e});
      if (Agent.LOG.isLoggable(Level.FINEST)) {
        Agent.LOG.log(Level.WARNING, msg, e);
      } else {
        Agent.LOG.warning(msg);
      }
    }
  }

  private void sampleMemoryPools(StatsEngine statsEngine) {
    try {
      List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
      for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
        PoolUsage poolUsage = new PoolUsage(memoryPoolMXBean);
        poolUsage.recordStats(statsEngine);
      }
    } catch (Exception e) {
      String msg = MessageFormat.format("An error occurred gathering memory pool metrics: {0}", new Object[] {e});
      if (Agent.LOG.isLoggable(Level.FINEST)) {
        Agent.LOG.log(Level.WARNING, msg, e);
      } else {
        Agent.LOG.warning(msg);
      }
    }
  }

  private static final class PoolUsage {
    private final String name;
    private final String type;
    private final long used;
    private final long committed;
    private final long max;

    private PoolUsage(MemoryPoolMXBean memoryPoolMXBean) {
      name = memoryPoolMXBean.getName();
      type = (memoryPoolMXBean.getType() == MemoryType.HEAP ? "Heap" : "Non-Heap");
      MemoryUsage memoryUsage = memoryPoolMXBean.getUsage();
      used = memoryUsage.getUsed();
      committed = memoryUsage.getCommitted();
      max = (memoryUsage.getMax() == -1L ? 0L : memoryUsage.getMax());
    }

    public void recordStats(StatsEngine statsEngine) {
      String metricName = MessageFormat.format("MemoryPool/{0}/{1}/Used", new Object[] {type, name});
      statsEngine.getStats(metricName).recordDataPoint(getUsed());
      metricName = MessageFormat.format("MemoryPool/{0}/{1}/Committed", new Object[] {type, name});
      statsEngine.getStats(metricName).recordDataPoint(getCommitted());
      metricName = MessageFormat.format("MemoryPool/{0}/{1}/Max", new Object[] {type, name});
      statsEngine.getStats(metricName).recordDataPoint(getMax());
    }

    private float getUsed() {
      return (float) used / 1048576.0F;
    }

    private float getCommitted() {
      return (float) committed / 1048576.0F;
    }

    private float getMax() {
      return (float) max / 1048576.0F;
    }
  }

  private static final class HeapAndNonHeapUsage {
    private final long heapUsed;
    private final long heapCommitted;
    private final long heapMax;
    private final long nonHeapUsed;
    private final long nonHeapCommitted;
    private final long nonHeapMax;

    private HeapAndNonHeapUsage(MemoryMXBean memoryMXBean) {
      MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
      nonHeapCommitted = nonHeapMemoryUsage.getCommitted();
      nonHeapUsed = nonHeapMemoryUsage.getUsed();
      nonHeapMax = (nonHeapMemoryUsage.getMax() == -1L ? 0L : nonHeapMemoryUsage.getMax());

      MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
      heapUsed = heapMemoryUsage.getUsed();
      heapCommitted = heapMemoryUsage.getCommitted();
      heapMax = (heapMemoryUsage.getMax() == -1L ? 0L : heapMemoryUsage.getMax());
    }

    public void recordStats(StatsEngine statsEngine) {
      statsEngine.getStats("Memory/Physical").recordDataPoint(getCommitted());
      statsEngine.getStats("Memory/Used").recordDataPoint(getUsed());
      statsEngine.getStats("Memory/Heap/Used").recordDataPoint(getHeapUsed());
      statsEngine.getStats("Memory/Heap/Committed").recordDataPoint(getHeapCommitted());
      statsEngine.getStats("Memory/Heap/Max").recordDataPoint(getHeapMax());
      statsEngine.getStats("Memory/Heap/Utilization").recordDataPoint(getHeapUtilization());
      statsEngine.getStats("Memory/NonHeap/Used").recordDataPoint(getNonHeapUsed());
      statsEngine.getStats("Memory/NonHeap/Committed").recordDataPoint(getNonHeapCommitted());
      statsEngine.getStats("Memory/NonHeap/Max").recordDataPoint(getNonHeapMax());
    }

    private float getCommitted() {
      return (float) (nonHeapCommitted + heapCommitted) / 1048576.0F;
    }

    private float getUsed() {
      return (float) (nonHeapUsed + heapUsed) / 1048576.0F;
    }

    private float getHeapUsed() {
      return (float) heapUsed / 1048576.0F;
    }

    private float getHeapUtilization() {
      return heapMax == 0L ? 0.0F : (float) heapUsed / (float) heapMax;
    }

    private float getHeapCommitted() {
      return (float) heapCommitted / 1048576.0F;
    }

    private float getHeapMax() {
      return (float) heapMax / 1048576.0F;
    }

    private float getNonHeapUsed() {
      return (float) nonHeapUsed / 1048576.0F;
    }

    private float getNonHeapCommitted() {
      return (float) nonHeapCommitted / 1048576.0F;
    }

    private float getNonHeapMax() {
      return (float) nonHeapMax / 1048576.0F;
    }
  }
}