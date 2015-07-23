package com.newrelic.agent.service.analytics;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.api.agent.Insights;

public class InsightsServiceImpl extends AbstractService implements InsightsService {
    private static final LoadingCache<String, String> stringCache =
            CacheBuilder.newBuilder().maximumSize(1000L).expireAfterAccess(70L, TimeUnit.SECONDS)
                    .build(new CacheLoader<String, String>() {
                        public String load(String key) throws Exception {
                            return key;
                        }
                    });
    private final boolean enabled;
    private final ConcurrentMap<String, Boolean> isEnabledForApp = new ConcurrentHashMap();
    protected final AgentConfigListener configListener = new AgentConfigListener() {
        public void configChanged(String appName, AgentConfig agentConfig) {
            isEnabledForApp.remove(appName);
        }
    };
    private final int maxSamplesStored;
    private final ConcurrentHashMap<String, ReserviorSampledArrayList<CustomInsightsEvent>> reservoirForApp =
            new ConcurrentHashMap();
    protected final HarvestListener harvestListener = new HarvestListener() {
        public void beforeHarvest(String appName, StatsEngine statsEngine) {
            harvest(appName, statsEngine);
        }

        public void afterHarvest(String appName) {
        }
    };
    protected final TransactionListener transactionListener = new TransactionListener() {
        public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
            TransactionInsights data = (TransactionInsights) transactionData.getInsightsData();
            InsightsServiceImpl.this.storeEvents(transactionData.getApplicationName(), data.events);
        }
    };

    public InsightsServiceImpl() {
        super("Insights");
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        maxSamplesStored = CustomInsightsEventsConfigUtils.getMaxSamplesStored(config);
        enabled = CustomInsightsEventsConfigUtils.isCustomInsightsEventsEnabled(config, maxSamplesStored);
        isEnabledForApp.put(config.getApplicationName(), Boolean.valueOf(enabled));
    }

    private static Map<String, Object> copyAndInternStrings(Map<String, Object> attributes) {
        Map result = Maps.newHashMap();
        for (Entry entry : attributes.entrySet()) {
            if ((entry.getValue() instanceof String)) {
                result.put(mapInternString((String) entry.getKey()), mapInternString((String) entry.getValue()));
            } else {
                result.put(mapInternString((String) entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    private static String mapInternString(String value) {
        try {
            return (String) stringCache.get(value);
        } catch (ExecutionException e) {
        }
        return value;
    }

    public boolean isEnabled() {
        return enabled;
    }

    protected void doStart() throws Exception {
        ServiceFactory.getHarvestService().addHarvestListener(harvestListener);
        ServiceFactory.getTransactionService().addTransactionListener(transactionListener);
        ServiceFactory.getConfigService().addIAgentConfigListener(configListener);
    }

    protected void doStop() throws Exception {
        ServiceFactory.getHarvestService().removeHarvestListener(harvestListener);
        ServiceFactory.getTransactionService().removeTransactionListener(transactionListener);
        ServiceFactory.getConfigService().removeIAgentConfigListener(configListener);
        reservoirForApp.clear();
        isEnabledForApp.clear();
        stringCache.invalidateAll();
    }

    public void recordCustomEvent(String eventType, Map<String, Object> attributes) {
        if (!enabled) {
            if (ServiceFactory.getConfigService().getDefaultAgentConfig().isHighSecurity()) {
                Agent.LOG.log(Level.FINER, "Event of type {0} not collected due to high security mode being enabled.",
                                     new Object[] {eventType});
            } else {
                Agent.LOG.log(Level.FINER, "Event of type {0} not collected. custom_insights_events not enabled.",
                                     new Object[] {eventType});
            }

            return;
        }
        if (AnalyticsEvent.isValidType(eventType)) {
            Transaction transaction = ServiceFactory.getTransactionService().getTransaction(false);
            if ((transaction == null) || (!transaction.isInProgress())) {
                String applicationName = ServiceFactory.getRPMService().getApplicationName();
                AgentConfig agentConfig = ServiceFactory.getConfigService().getAgentConfig(applicationName);
                if (!getIsEnabledForApp(agentConfig, applicationName)) {
                    reservoirForApp.remove(applicationName);
                    return;
                }
                storeEvent(applicationName, eventType, attributes);
            } else {
                transaction.getInsightsData().recordCustomEvent(eventType, attributes);
            }
        } else {
            Agent.LOG.log(Level.WARNING,
                                 "Custom event with invalid type of {0} was reported but ignored. Event types must "
                                         + "match /^[a-zA-Z0-9:_ ]+$/ and be less than 256 chars.",
                                 new Object[] {eventType});
        }
    }

    private void storeEvents(String appName, Collection<CustomInsightsEvent> events) {
        ReserviorSampledArrayList eventList;
        if (events.size() > 0) {
            eventList = getReservoir(appName);
            for (CustomInsightsEvent event : events) {
                Integer slot = eventList.getSlot();
                if (slot != null) {
                    eventList.set(slot.intValue(), event);
                }
            }
        }
    }

    public void storeEvent(String appName, CustomInsightsEvent event) {
        ReserviorSampledArrayList eventList = getReservoir(appName);
        Integer slot = eventList.getSlot();
        if (slot != null) {
            eventList.set(slot.intValue(), event);
            Agent.LOG.finest(MessageFormat.format("Added Custom Event of type {0}", new Object[] {event.type}));
        }
    }

    private void storeEvent(String appName, String eventType, Map<String, Object> attributes) {
        ReserviorSampledArrayList eventList = getReservoir(appName);
        Integer slot = eventList.getSlot();
        if (slot != null) {
            eventList.set(slot.intValue(),
                                 new CustomInsightsEvent(mapInternString(eventType), System.currentTimeMillis(),
                                                                copyAndInternStrings(attributes)));

            Agent.LOG.finest(MessageFormat.format("Added Custom Event of type {0}", new Object[] {eventType}));
        }
    }

    private ReserviorSampledArrayList<CustomInsightsEvent> getReservoir(String appName) {
        ReserviorSampledArrayList result = (ReserviorSampledArrayList) reservoirForApp.get(appName);
        while (result == null) {
            reservoirForApp.putIfAbsent(appName, new ReserviorSampledArrayList(maxSamplesStored));
            result = (ReserviorSampledArrayList) reservoirForApp.get(appName);
        }
        return result;
    }

    void harvest(String appName, StatsEngine statsEngine) {
        if (!getIsEnabledForApp(ServiceFactory.getConfigService().getAgentConfig(appName), appName)) {
            reservoirForApp.remove(appName);
            return;
        }

        ReserviorSampledArrayList reservoir = (ReserviorSampledArrayList) reservoirForApp.put(appName,
                                                                                                     new ReserviorSampledArrayList(maxSamplesStored));

        if ((reservoir != null) && (reservoir.size() > 0)) {
            try {
                ServiceFactory.getRPMService(appName)
                        .sendCustomAnalyticsEvents(Collections.unmodifiableList(reservoir));
                statsEngine.getStats("Supportability/Events/Customer/Sent").incrementCallCount(reservoir.size());
                statsEngine.getStats("Supportability/Events/Customer/Seen")
                        .incrementCallCount(reservoir.getNumberOfTries());

                if (reservoir.size() < reservoir.getNumberOfTries()) {
                    Agent.LOG.log(Level.WARNING, "Dropped {0} custom events out of {1}.",
                                         new Object[] {Integer.valueOf(reservoir.getNumberOfTries() - reservoir.size()),
                                                              Integer.valueOf(reservoir.getNumberOfTries())});
                }
            } catch (Exception e) {
                Agent.LOG.fine("Unable to send custom events. Unsent events will be included in the next harvest.");

                ReserviorSampledArrayList currentReservoir = (ReserviorSampledArrayList) reservoirForApp.get(appName);
                currentReservoir.addAll(reservoir);
            }
        }
    }

    private boolean getIsEnabledForApp(AgentConfig config, String currentAppName) {
        Boolean appEnabled = currentAppName == null ? null : (Boolean) isEnabledForApp.get(currentAppName);
        if (appEnabled == null) {
            appEnabled = Boolean.valueOf(CustomInsightsEventsConfigUtils.isCustomInsightsEventsEnabled(config,
                                                                                                              CustomInsightsEventsConfigUtils
                                                                                                                      .getMaxSamplesStored(config)));

            isEnabledForApp.put(currentAppName, appEnabled);
        }
        return appEnabled.booleanValue();
    }

    public Insights getTransactionInsights(AgentConfig config) {
        return new TransactionInsights(config);
    }

    static final class TransactionInsights implements Insights {
        final LinkedBlockingQueue<CustomInsightsEvent> events;

        TransactionInsights(AgentConfig config) {
            int maxSamplesStored = CustomInsightsEventsConfigUtils.getMaxSamplesStored(config);
            events = new LinkedBlockingQueue(maxSamplesStored);
        }

        public void recordCustomEvent(String eventType, Map<String, Object> attributes) {
            if (ServiceFactory.getConfigService().getDefaultAgentConfig().isHighSecurity()) {
                Agent.LOG.log(Level.FINER, "Event of type {0} not collected due to high security mode being enabled.",
                                     new Object[] {eventType});

                return;
            }
            if (AnalyticsEvent.isValidType(eventType)) {
                CustomInsightsEvent event = new CustomInsightsEvent(InsightsServiceImpl.mapInternString(eventType),
                                                                           System.currentTimeMillis(),
                                                                           InsightsServiceImpl
                                                                                   .copyAndInternStrings(attributes));

                if (events.offer(event)) {
                    Agent.LOG.finest(MessageFormat.format("Added Custom Event of type {0} in Transaction.",
                                                                 new Object[] {eventType}));
                } else {
                    String applicationName = ServiceFactory.getRPMService().getApplicationName();
                    ServiceFactory.getServiceManager().getInsights().storeEvent(applicationName, event);
                }
            } else {
                Agent.LOG.log(Level.WARNING, "Custom event with invalid type of {0} was reported for a transaction but "
                                                     + "ignored. Event types must match /^[a-zA-Z0-9:_ ]+$/ and be "
                                                     + "less than "
                                                     + "256 chars.", new Object[] {eventType});
            }
        }
    }
}