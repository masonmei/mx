package com.newrelic.agent.service.analytics;

import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.newrelic.deps.com.google.common.cache.CacheBuilder;
import com.newrelic.deps.com.google.common.cache.CacheLoader;
import com.newrelic.deps.com.google.common.cache.LoadingCache;
import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.attributes.AttributesUtils;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.Service;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.AbstractStats;
import com.newrelic.agent.stats.CountStats;
import com.newrelic.agent.stats.StatsBase;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.transaction.PriorityTransactionName;

public class TransactionEventsService extends AbstractService implements Service, HarvestListener, TransactionListener,
                                                                                 AgentConfigListener {
    static final int MAX_UNSENT_SYNTHETICS_HOLDERS = 25;
    static final int MAX_SYNTHETIC_EVENTS_PER_APP = 200;
    final ArrayDeque<FixedSizeArrayList<TransactionEvent>> pendingSyntheticsArrays = new ArrayDeque();
    private final boolean enabled;
    private final int maxSamplesStored;
    private final ConcurrentHashMap<String, ReserviorSampledArrayList<TransactionEvent>> reservoirForApp =
            new ConcurrentHashMap();
    private final ConcurrentHashMap<String, FixedSizeArrayList<TransactionEvent>> syntheticsListForApp =
            new ConcurrentHashMap();
    private final ConcurrentMap<String, Boolean> isEnabledForApp = new ConcurrentHashMap();
    private final LoadingCache<String, String> transactionNameCache;

    public TransactionEventsService() {
        super(TransactionEventsService.class.getSimpleName());

        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        maxSamplesStored = TransactionEventsConfigUtils.getMaxSamplesStored(config);
        enabled = TransactionEventsConfigUtils.isTransactionEventsEnabled(config, maxSamplesStored);
        isEnabledForApp.put(config.getApplicationName(), Boolean.valueOf(enabled));
        transactionNameCache =
                CacheBuilder.newBuilder().maximumSize(maxSamplesStored).expireAfterAccess(5L, TimeUnit.MINUTES)
                        .build(new CacheLoader<String, String>() {
                            public String load(String key) throws Exception {
                                return key;
                            }
                        });
    }

    public final boolean isEnabled() {
        return enabled;
    }

    protected void doStart() throws Exception {
        if (enabled) {
            ServiceFactory.getHarvestService().addHarvestListener(this);
            ServiceFactory.getTransactionService().addTransactionListener(this);
            ServiceFactory.getConfigService().addIAgentConfigListener(this);
        }
    }

    protected void doStop() throws Exception {
        ServiceFactory.getHarvestService().removeHarvestListener(this);
        ServiceFactory.getTransactionService().removeTransactionListener(this);
        ServiceFactory.getConfigService().removeIAgentConfigListener(this);
        reservoirForApp.clear();
    }

    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        beforeHarvestSynthetics(appName, statsEngine);

        List reservoirToSend = (List) reservoirForApp.put(appName, new ReserviorSampledArrayList(maxSamplesStored));

        if ((reservoirToSend != null) && (reservoirToSend.size() > 0)) {
            try {
                ServiceFactory.getRPMService(appName)
                        .sendAnalyticsEvents(Collections.unmodifiableList(reservoirToSend));
            } catch (Exception e) {
                Agent.LOG.fine("Unable to send events for regular transactions. This operation will be retried.");

                ReserviorSampledArrayList currentReservoir = (ReserviorSampledArrayList) reservoirForApp.get(appName);
                currentReservoir.addAll(reservoirToSend);
            }
        }
    }

    private void beforeHarvestSynthetics(String appName, StatsEngine statsEngine) {
        FixedSizeArrayList current =
                (FixedSizeArrayList) syntheticsListForApp.put(appName, new FixedSizeArrayList(200));

        if ((current != null) && (current.size() > 0)) {
            if (pendingSyntheticsArrays.size() < 25) {
                pendingSyntheticsArrays.add(current);
            } else {
                Agent.LOG.fine("Some synthetic transaction events were discarded.");
            }

        }

        int maxToSend = 5;
        for (int nSent = 0; nSent < 5; nSent++) {
            FixedSizeArrayList toSend = (FixedSizeArrayList) pendingSyntheticsArrays.poll();
            if (toSend == null) {
                break;
            }
            try {
                ServiceFactory.getRPMService(appName).sendAnalyticsEvents(Collections.unmodifiableList(toSend));
                nSent++;
            } catch (Exception e) {
                Agent.LOG.fine("Unable to send events for synthetic transactions. This operation will be retried.");
                pendingSyntheticsArrays.add(toSend);
                break;
            }
        }
    }

    public void afterHarvest(String appName) {
    }

    private boolean getIsEnabledForApp(AgentConfig config, String currentAppName) {
        Boolean appEnabled = (Boolean) isEnabledForApp.get(currentAppName);
        if (appEnabled == null) {
            appEnabled = Boolean.valueOf(TransactionEventsConfigUtils.isTransactionEventsEnabled(config,
                                                                                                        TransactionEventsConfigUtils
                                                                                                                .getMaxSamplesStored(config)));

            isEnabledForApp.put(currentAppName, appEnabled);
        }
        return appEnabled.booleanValue();
    }

    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        String name = transactionData.getApplicationName();

        if (!getIsEnabledForApp(transactionData.getAgentConfig(), name)) {
            reservoirForApp.remove(name);
            return;
        }

        boolean persisted = false;

        if (transactionData.isSyntheticTransaction()) {
            FixedSizeArrayList currentSyntheticsList = (FixedSizeArrayList) syntheticsListForApp.get(name);
            while (currentSyntheticsList == null) {
                syntheticsListForApp.putIfAbsent(name, new FixedSizeArrayList(200));

                currentSyntheticsList = (FixedSizeArrayList) syntheticsListForApp.get(name);
            }
            persisted = currentSyntheticsList.add(createEvent(transactionData, transactionStats));
            String msg =
                    MessageFormat.format("Added Synthetics transaction event: {0}", new Object[] {transactionData});
            Agent.LOG.finest(msg);
        }

        if (!persisted) {
            ReserviorSampledArrayList currentReservoir = (ReserviorSampledArrayList) reservoirForApp.get(name);
            while (currentReservoir == null) {
                reservoirForApp.putIfAbsent(name, new ReserviorSampledArrayList(maxSamplesStored));
                currentReservoir = (ReserviorSampledArrayList) reservoirForApp.get(name);
            }
            Integer slot = currentReservoir.getSlot();
            if (slot != null) {
                currentReservoir.set(slot.intValue(), createEvent(transactionData, transactionStats));
            }
        }
    }

    private TransactionEvent createEvent(TransactionData transactionData, TransactionStats transactionStats) {
        long startTime = transactionData.getWallClockStartTimeMs();
        String metricName = transactionData.getBlameOrRootMetricName();
        try {
            metricName = (String) transactionNameCache.get(metricName);
        } catch (ExecutionException e) {
            Agent.LOG.finest("Error fetching cached transaction name: " + e.toString());
        }
        long durationInNanos = transactionData.getDuration();

        Integer port = ServiceFactory.getEnvironmentService().getEnvironment().getAgentIdentity().getServerPort();

        String subType = "Web";
        if (!transactionData.isWebTransaction()) {
            PriorityTransactionName transactionName = transactionData.getPriorityTransactionName();
            String otherCategory = transactionName.getCategory();
            if (otherCategory != null) {
                subType = otherCategory;
            }
        }
        TransactionEvent event =
                new TransactionEvent(transactionData.getApplicationName(), subType, startTime, metricName,
                                            (float) durationInNanos / 1.0E+09F, transactionData.getGuid(),
                                            transactionData.getReferrerGuid(), port, transactionData.getTripId(),
                                            transactionData.getReferringPathHash(),
                                            transactionData.getAlternatePathHashes(),
                                            transactionData.getApdexPerfZone(),
                                            transactionData.getSyntheticsResourceId(),
                                            transactionData.getSyntheticsMonitorId(),
                                            transactionData.getSyntheticsJobId());

        if (transactionData.getTripId() != null) {
            event.pathHash = Integer.valueOf(transactionData.generatePathHash());
        }
        event.queueDuration = retrieveMetricIfExists(transactionStats, "WebFrontend/QueueTime").getTotal();
        event.externalDuration = retrieveMetricIfExists(transactionStats, "External/all").getTotal();
        event.externalCallCount = retrieveMetricIfExists(transactionStats, "External/all").getCallCount();

        event.databaseDuration = retrieveMetricIfExists(transactionStats, "Datastore/all").getTotal();
        event.databaseCallCount = retrieveMetricIfExists(transactionStats, "Datastore/all").getCallCount();

        event.gcCumulative = retrieveMetricIfExists(transactionStats, "GC/cumulative").getTotal();
        if (ServiceFactory.getAttributesService().isAttributesEnabledForEvents(transactionData.getApplicationName())) {
            event.userAttributes = transactionData.getUserAttributes();
            event.agentAttributes = transactionData.getAgentAttributes();

            event.agentAttributes
                    .putAll(AttributesUtils.appendAttributePrefixes(transactionData.getPrefixedAttributes()));
        }

        return event;
    }

    private CountStats retrieveMetricIfExists(TransactionStats transactionStats, String metricName) {
        if (!transactionStats.getUnscopedStats().getStatsMap().containsKey(metricName)) {
            return NoCallCountStats.NO_STATS;
        }
        return transactionStats.getUnscopedStats().getResponseTimeStats(metricName);
    }

    public void configChanged(String appName, AgentConfig agentConfig) {
        isEnabledForApp.remove(appName);
    }

    public ReserviorSampledArrayList<TransactionEvent> unsafeGetEventData(String appName) {
        return (ReserviorSampledArrayList) reservoirForApp.get(appName);
    }

    private static class NoCallCountStats extends AbstractStats {
        static final NoCallCountStats NO_STATS = new NoCallCountStats();

        public float getTotal() {
            return (1.0F / -1.0F);
        }

        public float getTotalExclusiveTime() {
            return (1.0F / -1.0F);
        }

        public float getMinCallTime() {
            return (1.0F / -1.0F);
        }

        public float getMaxCallTime() {
            return (1.0F / -1.0F);
        }

        public double getSumOfSquares() {
            return (-1.0D / 0.0D);
        }

        public boolean hasData() {
            return false;
        }

        public void reset() {
        }

        public void merge(StatsBase stats) {
        }

        public Object clone() throws CloneNotSupportedException {
            return NO_STATS;
        }
    }
}