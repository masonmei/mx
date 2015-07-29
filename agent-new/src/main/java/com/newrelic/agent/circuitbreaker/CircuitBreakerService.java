package com.newrelic.agent.circuitbreaker;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.RPMService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.CircuitBreakerConfig;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;

public class CircuitBreakerService extends AbstractService implements HarvestListener, AgentConfigListener {
    private static final int TRACER_SAMPLING_RATE = 1000;
    private final CircuitBreakerConfig circuitBreakerConfig;
    private final ReentrantLock lock = new ReentrantLock();
    private final ConcurrentMap<String, Boolean> missingData;
    private final ThreadLocal<Boolean> logWarning = new ThreadLocal() {
        protected Boolean initialValue() {
            return Boolean.valueOf(true);
        }
    };
    private final ThreadLocal<Long> lastTotalGCTimeNS = new ThreadLocal() {
        protected Long initialValue() {
            return Long.valueOf(0L);
        }
    };
    private final ThreadLocal<Long> lastTotalCpuTimeNS = new ThreadLocal() {
        protected Long initialValue() {
            return Long.valueOf(0L);
        }
    };
    private final ThreadLocal<SamplingCounter> tracerSamplerCounter = new ThreadLocal() {
        protected SamplingCounter initialValue() {
            return CircuitBreakerService.createTracerSamplerCounter();
        }
    };
    private volatile int tripped = 0;
    private volatile GarbageCollectorMXBean oldGenGCBeanCached = null;

    public CircuitBreakerService() {
        super(CircuitBreakerService.class.getSimpleName());

        this.circuitBreakerConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getCircuitBreakerConfig();

        if ((isEnabled()) && ((null == getOldGenGCBean()) || (getCpuTimeNS() < 0L))) {
            Agent.LOG.log(Level.WARNING,
                                 "Circuit breaker: Missing required JMX beans. Cannot enable circuit breaker. GC "
                                         + "bean: {0} CpuTime: {1}",
                                 new Object[] {getOldGenGCBean(), Long.valueOf(getCpuTimeNS())});

            this.circuitBreakerConfig.updateEnabled(false);
        }
        ServiceFactory.getConfigService().addIAgentConfigListener(this);
        this.missingData = new ConcurrentHashMap();
    }

    public static SamplingCounter createTracerSamplerCounter() {
        return new SamplingCounter(1000L);
    }

    public boolean isEnabled() {
        return this.circuitBreakerConfig.isEnabled();
    }

    protected void doStart() throws Exception {
        ServiceFactory.getHarvestService().addHarvestListener(this);
    }

    protected void doStop() throws Exception {
        ServiceFactory.getConfigService().removeIAgentConfigListener(this);
        ServiceFactory.getHarvestService().removeHarvestListener(this);
    }

    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        this.lastTotalCpuTimeNS.set(Long.valueOf(getCpuTimeNS()));
        this.lastTotalGCTimeNS.set(Long.valueOf(getGCCpuTimeNS()));

        if ((this.missingData.containsKey(appName)) && (((Boolean) this.missingData.get(appName)).booleanValue())) {
            recordBreakerOnMetrics(statsEngine, "AgentCheck/CircuitBreaker/tripped/memory");
        } else {
            recordBreakerOffMetrics(statsEngine);
        }
    }

    private void recordBreakerOnMetrics(StatsEngine statsEngine, String tripCauseMetric) {
        statsEngine.getStats("AgentCheck/CircuitBreaker/tripped/all").incrementCallCount();
        statsEngine.getStats(tripCauseMetric).incrementCallCount();
    }

    private void recordBreakerOffMetrics(StatsEngine statsEngine) {
        statsEngine.recordEmptyStats("AgentCheck/CircuitBreaker/tripped/all");
    }

    public void afterHarvest(String appName) {
        if ((isTripped()) && (shouldReset())) {
            reset();
        }
        if (!isTripped()) {
            this.missingData.put(appName, Boolean.valueOf(false));

            if (isTripped()) {
                this.missingData.put(appName, Boolean.valueOf(true));
            }
        }
    }

    private boolean shouldTrip() {
        if (!isEnabled()) {
            return false;
        }
        long cpuTime = getCpuTimeNS() - ((Long) this.lastTotalCpuTimeNS.get()).longValue();
        long gcCpuTime = getGCCpuTimeNS() - ((Long) this.lastTotalGCTimeNS.get()).longValue();
        long totalTime = cpuTime + gcCpuTime;
        double gcCpuTimePercentage = gcCpuTime / totalTime * 100.0D;

        if (cpuTime <= 0L) {
            return false;
        }
        double percentageFreeMemory =
                100.0D * ((Runtime.getRuntime().freeMemory() + (Runtime.getRuntime().maxMemory() - Runtime.getRuntime()
                                                                                                           .totalMemory()))
                                  / Runtime.getRuntime().maxMemory());

        this.lastTotalCpuTimeNS.set(Long.valueOf(((Long) this.lastTotalCpuTimeNS.get()).longValue() + cpuTime));
        this.lastTotalGCTimeNS.set(Long.valueOf(((Long) this.lastTotalGCTimeNS.get()).longValue() + gcCpuTime));

        int freeMemoryThreshold = this.circuitBreakerConfig.getMemoryThreshold();
        int gcCPUThreshold = this.circuitBreakerConfig.getGcCpuThreshold();
        Agent.LOG.log(Level.FINEST,
                             "Circuit breaker: percentage free memory {0}%  GC CPU time percentage {1}% "
                                     + "(freeMemoryThreshold {2}, gcCPUThreshold {3})",
                             new Object[] {Double.valueOf(percentageFreeMemory), Double.valueOf(gcCpuTimePercentage),
                                                  Integer.valueOf(freeMemoryThreshold),
                                                  Integer.valueOf(gcCPUThreshold)});

        if ((gcCpuTimePercentage >= gcCPUThreshold) && (percentageFreeMemory <= freeMemoryThreshold)) {
            Agent.LOG.log(Level.FINE, "Circuit breaker tripped at memory {0}%  GC CPU time {1}%",
                                 new Object[] {Double.valueOf(percentageFreeMemory),
                                                      Double.valueOf(gcCpuTimePercentage)});

            return true;
        }
        return false;
    }

    private boolean shouldReset() {
        return !shouldTrip();
    }

    public boolean isTripped() {
        if ((((SamplingCounter) this.tracerSamplerCounter.get()).shouldSample()) && (this.tripped == 0)) {
            checkAndTrip();
        }
        return this.tripped == 1;
    }

    private void trip() {
        this.tripped = 1;

        for (String appName : this.missingData.keySet()) {
            this.missingData.put(appName, Boolean.valueOf(true));
        }

        if (((Boolean) this.logWarning.get()).booleanValue()) {
            this.logWarning.set(Boolean.valueOf(false));
            Agent.LOG.log(Level.WARNING,
                                 "Circuit breaker tripped. The agent ceased to create transaction data to perserve "
                                         + "heap memory. This may cause incomplete transaction data in the APM UI.");
        }
    }

    public void reset() {
        this.tripped = 0;
        Agent.LOG.log(Level.FINE, "Circuit breaker reset");
        this.logWarning.set(Boolean.valueOf(true));
    }

    public boolean checkAndTrip() {
        if (this.lock.tryLock()) {
            try {
                if ((!isTripped()) && (shouldTrip())) {
                    trip();
                    return true;
                }
            } finally {
                this.lock.unlock();
            }
        }
        return false;
    }

    private long getCpuTimeNS() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        if ((!threadMXBean.isThreadCpuTimeSupported()) || (!threadMXBean.isThreadCpuTimeEnabled())) {
            return -1L;
        }

        long cpuTime = 0L;
        long threadCpuTime = 0L;
        for (long id : threadMXBean.getAllThreadIds()) {
            threadCpuTime = threadMXBean.getThreadCpuTime(id);
            if (threadCpuTime != -1L) {
                cpuTime += threadCpuTime;
            }
        }
        return cpuTime;
    }

    private long getGCCpuTimeNS() {
        return TimeUnit.NANOSECONDS.convert(getOldGenGCBean().getCollectionTime(), TimeUnit.MILLISECONDS);
    }

    private long getGCCount() {
        long gcCpuCount = 0L;
        long collectorCount = 0L;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            collectorCount = gcBean.getCollectionCount();
            if (collectorCount != -1L) {
                gcCpuCount += gcBean.getCollectionCount();
            }
        }
        return gcCpuCount;
    }

    private GarbageCollectorMXBean getOldGenGCBean() {
        if (null != this.oldGenGCBeanCached) {
            return this.oldGenGCBeanCached;
        }
        synchronized(this) {
            if (null != this.oldGenGCBeanCached) {
                return this.oldGenGCBeanCached;
            }

            GarbageCollectorMXBean lowestGCCountBean = null;
            Agent.LOG.log(Level.FINEST, "Circuit breaker: looking for old gen gc bean");

            boolean tie = false;
            long totalGCs = getGCCount();

            for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
                Agent.LOG.log(Level.FINEST, "Circuit breaker: checking {0}", new Object[] {gcBean.getName()});
                if ((null == lowestGCCountBean) || (lowestGCCountBean.getCollectionCount()
                                                            > gcBean.getCollectionCount())) {
                    tie = false;
                    lowestGCCountBean = gcBean;
                } else if (lowestGCCountBean.getCollectionCount() == gcBean.getCollectionCount()) {
                    tie = true;
                }
            }
            if ((getGCCount() == totalGCs) && (!tie)) {
                Agent.LOG.log(Level.FINEST, "Circuit breaker: found and cached oldGenGCBean: {0}",
                                     new Object[] {lowestGCCountBean.getName()});

                this.oldGenGCBeanCached = lowestGCCountBean;
                return this.oldGenGCBeanCached;
            }
            Agent.LOG.log(Level.FINEST, "Circuit breaker: unable to find oldGenGCBean. Best guess: {0}",
                                 new Object[] {lowestGCCountBean.getName()});

            return lowestGCCountBean;
        }
    }

    public void configChanged(String appName, AgentConfig agentConfig) {
        int newGCCpuThreshold = agentConfig.getCircuitBreakerConfig().getGcCpuThreshold();
        int newMemoryThreshold = agentConfig.getCircuitBreakerConfig().getMemoryThreshold();
        boolean newEnabled = agentConfig.getCircuitBreakerConfig().isEnabled();

        if ((newGCCpuThreshold == this.circuitBreakerConfig.getGcCpuThreshold()) && (newMemoryThreshold
                                                                                             == this.circuitBreakerConfig
                                                                                                        .getMemoryThreshold())
                    && (newEnabled == this.circuitBreakerConfig.isEnabled())) {
            return;
        }

        this.circuitBreakerConfig.updateEnabled(newEnabled);
        this.circuitBreakerConfig.updateThresholds(newGCCpuThreshold, newMemoryThreshold);
        Agent.LOG.log(Level.INFO,
                             "Circuit breaker: updated configuration - enabled {0} GC CPU Threshold {1}% Memory Threshold {2}%.",
                             new Object[] {Boolean.valueOf(this.circuitBreakerConfig.isEnabled()),
                                                  Integer.valueOf(this.circuitBreakerConfig.getGcCpuThreshold()),
                                                  Integer.valueOf(this.circuitBreakerConfig.getMemoryThreshold())});
    }

    public void addRPMService(RPMService rpmService) {
        this.missingData.put(rpmService.getApplicationName(), Boolean.valueOf(isTripped()));
    }

    public void removeRPMService(RPMService rpmService) {
        this.missingData.remove(rpmService.getApplicationName());
    }

    public void setPreviousChecksForTesting(long newGCTimeNS, long newCpuTimeNS) {
        this.lastTotalGCTimeNS.set(Long.valueOf(newGCTimeNS));
        this.lastTotalCpuTimeNS.set(Long.valueOf(newCpuTimeNS));
    }
}