package com.newrelic.agent.stats;

public abstract interface CountStats extends StatsBase
{
  public abstract void incrementCallCount();

  public abstract void incrementCallCount(int paramInt);

  public abstract int getCallCount();

  public abstract void setCallCount(int paramInt);

  public abstract float getTotal();

  public abstract float getTotalExclusiveTime();

  public abstract float getMinCallTime();

  public abstract float getMaxCallTime();

  public abstract double getSumOfSquares();
}