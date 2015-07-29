package com.newrelic.agent.service.analytics;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;

abstract class TransactionEventsConfigUtils {
    public static final int DEFAULT_MAX_SAMPLES_STORED = 2000;
    public static final boolean DEFAULT_ENABLED = true;

    static boolean isTransactionEventsEnabled(AgentConfig config, int maxSamplesStored) {
        return (maxSamplesStored > 0) && (((Boolean) config.getValue("analytics_events.enabled", Boolean.valueOf(true)))
                                                  .booleanValue())
                       && (((Boolean) config.getValue("transaction_events.enabled", Boolean.valueOf(true)))
                                   .booleanValue())
                       && (((Boolean) config.getValue("transaction_events.collect_analytics_events",
                                                             Boolean.valueOf(true))).booleanValue());
    }

    static int getMaxSamplesStored(AgentConfig config) {
        Integer newMax = (Integer) config.getValue("transaction_events.max_samples_stored");
        if (newMax != null) {
            return newMax.intValue();
        }

        Integer oldMax = (Integer) config.getValue("analytics_events.max_samples_stored", Integer.valueOf(2000));
        if (oldMax != null) {
            Agent.LOG.info("The property analytics_events.max_samples_stored is deprecated. Please use "
                                   + "transaction_events.max_samples_stored.");
            return oldMax.intValue();
        }

        return 2000;
    }
}