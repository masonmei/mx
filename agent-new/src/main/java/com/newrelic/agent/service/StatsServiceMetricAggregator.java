package com.newrelic.agent.service;

import com.newrelic.agent.stats.AbstractMetricAggregator;
import com.newrelic.agent.stats.RecordResponseTimeMetric;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsWorks;
import java.util.concurrent.TimeUnit;

public class StatsServiceMetricAggregator extends AbstractMetricAggregator
{
  private final StatsService statsService;

  public StatsServiceMetricAggregator(StatsService statsService)
  {
    this.statsService = statsService;
  }

  protected void doRecordResponseTimeMetric(String name, long totalTime, long exclusiveTime, TimeUnit timeUnit)
  {
    this.statsService.doStatsWork(new RecordResponseTimeMetric(totalTime, exclusiveTime, name, timeUnit));
  }

  protected void doRecordMetric(String name, float value)
  {
    this.statsService.doStatsWork(StatsWorks.getRecordMetricWork(name, value));
  }

  protected void doIncrementCounter(String name, int count)
  {
    this.statsService.doStatsWork(StatsWorks.getIncrementCounterWork(name, count));
  }
}