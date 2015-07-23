package com.newrelic.agent.api;

import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.Stats;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsWork;

@Deprecated
public class AgentAPI {
    @Deprecated
    public static void recordValue(final String name, final float value) {
        ServiceFactory.getStatsService().doStatsWork(new StatsWork() {
            public String getAppName() {
                return null;
            }

            public void doWork(StatsEngine statsEngine) {
                statsEngine.getStats(name).recordDataPoint(value);
            }
        });
    }

    @Deprecated
    public static Stats getStats(String name) {
        String msg = "AgentAPI#getStats(String) is not supported. Use com.newrelic.api.agent.NewRelic";
        throw new UnsupportedOperationException(msg);
    }
}