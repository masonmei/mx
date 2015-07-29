package com.newrelic.agent;

import com.newrelic.agent.stats.StatsEngine;

public abstract interface HarvestListener
{
  public abstract void beforeHarvest(String paramString, StatsEngine paramStatsEngine);

  public abstract void afterHarvest(String paramString);
}