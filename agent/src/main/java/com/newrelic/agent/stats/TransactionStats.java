package com.newrelic.agent.stats;

public class TransactionStats {
    private final SimpleStatsEngine unscopedStats = new SimpleStatsEngine(16);
    private final SimpleStatsEngine scopedStats = new SimpleStatsEngine();

    public SimpleStatsEngine getUnscopedStats() {
        return unscopedStats;
    }

    public SimpleStatsEngine getScopedStats() {
        return scopedStats;
    }

    public int getSize() {
        return unscopedStats.getStatsMap().size() + scopedStats.getStatsMap().size();
    }

    public String toString() {
        return "TransactionStats [unscopedStats=" + unscopedStats + ", scopedStats=" + scopedStats + "]";
    }
}