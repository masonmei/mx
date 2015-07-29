package com.newrelic.agent.sql;

import java.util.Map;

public abstract interface SqlTrace
{
  public abstract int getId();

  public abstract String getSql();

  public abstract int getCallCount();

  public abstract long getTotal();

  public abstract long getMax();

  public abstract long getMin();

  public abstract String getBlameMetricName();

  public abstract String getUri();

  public abstract String getMetricName();

  public abstract Map<String, Object> getParameters();
}