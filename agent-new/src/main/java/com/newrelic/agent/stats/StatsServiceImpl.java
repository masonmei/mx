package com.newrelic.agent.stats;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.newrelic.agent.Agent;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.StatsServiceMetricAggregator;
import com.newrelic.api.agent.MetricAggregator;

public class StatsServiceImpl extends AbstractService implements StatsService {
    private final MetricAggregator metricAggregator = new StatsServiceMetricAggregator(this);

    private final ConcurrentMap<String, StatsEngineQueue> statsEngineQueues = new ConcurrentHashMap();
    private final String defaultAppName;
    private volatile StatsEngineQueue defaultStatsEngineQueue;

    public StatsServiceImpl() {
        super(StatsService.class.getSimpleName());
        defaultAppName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
        defaultStatsEngineQueue = createStatsEngineQueue();
    }

    public boolean isEnabled() {
        return true;
    }

    protected void doStart() {
    }

    protected void doStop() {
    }

    public void doStatsWork(StatsWork work) {
        String appName = work.getAppName();
        boolean done = false;
        while (!done) {
            done = getOrCreateStatsEngineQueue(appName).doStatsWork(work);
        }
    }

    public StatsEngine getStatsEngineForHarvest(String appName) {
        StatsEngineQueue oldStatsEngineQueue = replaceStatsEngineQueue(appName);
        oldStatsEngineQueue.close();
        return oldStatsEngineQueue.getStatsEngineForHarvest();
    }

    public MetricAggregator getMetricAggregator() {
        return metricAggregator;
    }

    private StatsEngineQueue replaceStatsEngineQueue(String appName) {
        StatsEngineQueue oldStatsEngineQueue = getOrCreateStatsEngineQueue(appName);
        StatsEngineQueue newStatsEngineQueue = createStatsEngineQueue();
        if (oldStatsEngineQueue == defaultStatsEngineQueue) {
            defaultStatsEngineQueue = newStatsEngineQueue;
        } else {
            statsEngineQueues.put(appName, newStatsEngineQueue);
        }
        return oldStatsEngineQueue;
    }

    private StatsEngineQueue getOrCreateStatsEngineQueue(String appName) {
        StatsEngineQueue statsEngineQueue = getStatsEngineQueue(appName);
        if (statsEngineQueue != null) {
            return statsEngineQueue;
        }
        statsEngineQueue = createStatsEngineQueue();
        StatsEngineQueue oldStatsEngineQueue =
                (StatsEngineQueue) statsEngineQueues.putIfAbsent(appName, statsEngineQueue);
        return oldStatsEngineQueue == null ? statsEngineQueue : oldStatsEngineQueue;
    }

    private StatsEngineQueue getStatsEngineQueue(String appName) {
        if ((appName == null) || (appName.equals(defaultAppName))) {
            return defaultStatsEngineQueue;
        }
        return (StatsEngineQueue) statsEngineQueues.get(appName);
    }

    private StatsEngineQueue createStatsEngineQueue() {
        return new StatsEngineQueue();
    }

    private static class StatsEngineQueue {
        private final BlockingQueue<StatsEngine> statsEngineQueue = new LinkedBlockingQueue();
        private final Lock readLock;
        private final Lock writeLock;
        private final AtomicInteger statsEngineCount = new AtomicInteger();
        private volatile boolean isClosed = false;

        private StatsEngineQueue() {
            ReadWriteLock lock = new ReentrantReadWriteLock();
            readLock = lock.readLock();
            writeLock = lock.writeLock();
        }

        public boolean doStatsWork(StatsWork work) {
            if (readLock.tryLock()) {
                try {
                    boolean bool;
                    if (isClosed()) {
                        return false;
                    }
                    doStatsWorkUnderLock(work);
                    return true;
                } finally {
                    readLock.unlock();
                }
            }
            return false;
        }

        private void doStatsWorkUnderLock(StatsWork work) {
            StatsEngine statsEngine = null;
            try {
                statsEngine = (StatsEngine) statsEngineQueue.poll();
                if (statsEngine == null) {
                    statsEngine = createStatsEngine();
                    statsEngineCount.incrementAndGet();
                }
                work.doWork(statsEngine);
            } catch (Exception e) {
                String msg = MessageFormat.format("Exception doing stats work: {0}", new Object[] {e});
                Agent.LOG.warning(msg);
            } finally {
                if (statsEngine != null) {
                    try {
                        if (!statsEngineQueue.offer(statsEngine)) {
                            Agent.LOG.warning("Failed to return stats engine to queue");
                        }
                    } catch (Exception e) {
                        String msg = MessageFormat.format("Exception returning stats engine to queue: {0}",
                                                                 new Object[] {e});
                        Agent.LOG.warning(msg);
                    }
                }
            }
        }

        public StatsEngine getStatsEngineForHarvest() {
            writeLock.lock();
            try {
                return getStatsEngineForHarvestUnderLock();
            } finally {
                writeLock.unlock();
            }
        }

        private StatsEngine getStatsEngineForHarvestUnderLock() {
            List<StatsEngine> statsEngines = new ArrayList<StatsEngine>();
            try {
                statsEngineQueue.drainTo(statsEngines);
            } catch (Exception e) {
                String msg = MessageFormat.format("Exception draining stats engine queue: {0}", new Object[] {e});
                Agent.LOG.warning(msg);
            }
            if (statsEngines.size() != statsEngineCount.get()) {
                String msg = MessageFormat.format("Error draining stats engine queue. Expected: {0} actual: {1}",
                                                         new Object[] {Integer.valueOf(statsEngineCount.get()),
                                                                              Integer.valueOf(statsEngines.size())});

                Agent.LOG.warning(msg);
            }

            StatsEngine harvestStatsEngine = createStatsEngine();
            for (StatsEngine statsEngine : statsEngines) {
                harvestStatsEngine.mergeStats(statsEngine);
            }
            return harvestStatsEngine;
        }

        private StatsEngine createStatsEngine() {
            return new StatsEngineImpl();
        }

        public void close() {
            isClosed = true;
        }

        private boolean isClosed() {
            return isClosed;
        }
    }
}