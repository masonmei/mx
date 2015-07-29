package com.newrelic.agent.stats;

public abstract interface Stats extends CountStats
{
  public abstract void recordDataPoint(float paramFloat);
}