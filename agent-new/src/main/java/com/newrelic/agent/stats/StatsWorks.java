package com.newrelic.agent.stats;

import java.util.concurrent.TimeUnit;

public class StatsWorks
{
  public static StatsWork getIncrementCounterWork(String name, int count)
  {
    return new IncrementCounter(name, count);
  }

  public static StatsWork getRecordMetricWork(String name, float value) {
    return new RecordMetric(name, value);
  }

  public static StatsWork getRecordResponseTimeWork(String name, long millis) {
    return new RecordResponseTimeMetric(millis, name, TimeUnit.MILLISECONDS);
  }
}