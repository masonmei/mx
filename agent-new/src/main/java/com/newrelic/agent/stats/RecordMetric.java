package com.newrelic.agent.stats;

final class RecordMetric
  implements StatsWork
{
  private final String name;
  private final float value;

  public RecordMetric(String name, float value)
  {
    this.name = name;
    this.value = value;
  }

  public void doWork(StatsEngine statsEngine)
  {
    statsEngine.getStats(this.name).recordDataPoint(this.value);
  }

  public String getAppName()
  {
    return null;
  }
}