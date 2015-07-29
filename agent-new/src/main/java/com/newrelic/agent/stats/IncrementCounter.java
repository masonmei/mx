package com.newrelic.agent.stats;

final class IncrementCounter
  implements StatsWork
{
  private final String name;
  private final int count;

  public IncrementCounter(String name, int count)
  {
    this.name = name;
    this.count = count;
  }

  public void doWork(StatsEngine statsEngine)
  {
    statsEngine.getStats(this.name).incrementCallCount(this.count);
  }

  public String getAppName()
  {
    return null;
  }
}