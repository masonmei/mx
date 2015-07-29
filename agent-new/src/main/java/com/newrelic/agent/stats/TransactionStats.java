package com.newrelic.agent.stats;

import java.util.Map;

public class TransactionStats
{
  private final SimpleStatsEngine unscopedStats = new SimpleStatsEngine(16);
  private final SimpleStatsEngine scopedStats = new SimpleStatsEngine();

  public SimpleStatsEngine getUnscopedStats() {
    return this.unscopedStats;
  }

  public SimpleStatsEngine getScopedStats()
  {
    return this.scopedStats;
  }

  public int getSize() {
    return this.unscopedStats.getStatsMap().size() + this.scopedStats.getStatsMap().size();
  }

  public String toString()
  {
    return "TransactionStats [unscopedStats=" + this.unscopedStats + ", scopedStats=" + this.scopedStats + "]";
  }
}