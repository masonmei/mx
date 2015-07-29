package com.newrelic.agent.samplers;

import com.newrelic.agent.stats.StatsEngine;

public abstract interface MetricSampler
{
  public abstract void sample(StatsEngine paramStatsEngine);
}