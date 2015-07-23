package com.newrelic.agent;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.StatsEngine;

public class GCService extends AbstractService implements HarvestListener {
    private final Map<String, GarbageCollector> garbageCollectors = new HashMap();

    public GCService() {
        super(GCService.class.getSimpleName());
    }

    public boolean isEnabled() {
        return true;
    }

    protected void doStart() {
        ServiceFactory.getHarvestService().addHarvestListener(this);
    }

    protected void doStop() {
    }

    public synchronized void beforeHarvest(String appName, StatsEngine statsEngine) {
        try {
            harvestGC(statsEngine);
        } catch (Exception e) {
            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg =
                        MessageFormat.format("Error harvesting GC metrics for {0}: {1}", new Object[] {appName, e});
                Agent.LOG.finer(msg);
            }
        }
    }

    private void harvestGC(StatsEngine statsEngine) {
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            GarbageCollector garbageCollector = (GarbageCollector) garbageCollectors.get(gcBean.getName());
            if (garbageCollector == null) {
                garbageCollector = new GarbageCollector(gcBean);
                garbageCollectors.put(gcBean.getName(), garbageCollector);
            } else {
                garbageCollector.recordGC(gcBean, statsEngine);
            }
        }
    }

    public void afterHarvest(String appName) {
    }

    private class GarbageCollector {
        private long collectionCount;
        private long collectionTime;

        public GarbageCollector(GarbageCollectorMXBean gcBean) {
            collectionCount = gcBean.getCollectionCount();
            collectionTime = gcBean.getCollectionTime();
        }

        private void recordGC(GarbageCollectorMXBean gcBean, StatsEngine statsEngine) {
            long lastCollectionCount = collectionCount;
            long lastCollectionTime = collectionTime;

            collectionCount = gcBean.getCollectionCount();
            collectionTime = gcBean.getCollectionTime();

            long numberOfCollections = collectionCount - lastCollectionCount;
            long time = collectionTime - lastCollectionTime;

            if (numberOfCollections > 0L) {
                String rootMetricName = "GC/" + gcBean.getName();
                ResponseTimeStats stats = statsEngine.getResponseTimeStats(rootMetricName);
                stats.recordResponseTime(time, TimeUnit.MILLISECONDS);
                stats.setCallCount((int) numberOfCollections);
            }
        }
    }
}