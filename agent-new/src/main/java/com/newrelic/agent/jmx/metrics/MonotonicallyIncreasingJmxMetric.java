package com.newrelic.agent.jmx.metrics;

import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.stats.MonotonicallyIncreasingStatsEngine;
import com.newrelic.agent.stats.StatsEngine;

public class MonotonicallyIncreasingJmxMetric extends JmxMetric
{
  private static final MonotonicallyIncreasingStatsEngine monoStatsEngine = new MonotonicallyIncreasingStatsEngine();

  public MonotonicallyIncreasingJmxMetric(String attribute) {
    super(attribute);
  }

  public MonotonicallyIncreasingJmxMetric(String[] attributes, String attName, JmxAction pAction) {
    super(attributes, attName, pAction);
  }

  public JmxType getType()
  {
    return JmxType.MONOTONICALLY_INCREASING;
  }

  public void recordStats(StatsEngine statsEngine, String name, float value)
  {
    monoStatsEngine.recordMonoStats(statsEngine, name, value);
  }
}