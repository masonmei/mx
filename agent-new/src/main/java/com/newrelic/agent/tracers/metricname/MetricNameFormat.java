package com.newrelic.agent.tracers.metricname;

public abstract interface MetricNameFormat
{
  public abstract String getMetricName();

  public abstract String getTransactionSegmentName();

  public abstract String getTransactionSegmentUri();
}