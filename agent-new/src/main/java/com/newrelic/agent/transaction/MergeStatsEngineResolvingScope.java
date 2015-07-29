package com.newrelic.agent.transaction;

import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsWork;
import com.newrelic.agent.stats.TransactionStats;

public class MergeStatsEngineResolvingScope implements StatsWork {
    private final String appName;
    private final TransactionStats statsEngine;
    private final String resolvedScope;

    public MergeStatsEngineResolvingScope(String resolvedScope, String appName, TransactionStats statsEngine) {
        this.resolvedScope = resolvedScope;
        this.appName = appName;
        this.statsEngine = statsEngine;
    }

    public void doWork(StatsEngine statsEngine) {
        statsEngine.mergeStatsResolvingScope(this.statsEngine, this.resolvedScope);
    }

    public String getAppName() {
        return this.appName;
    }
}