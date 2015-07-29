package com.newrelic.agent.tracers.metricname;

public abstract class AbstractMetricNameFormat
  implements MetricNameFormat
{
  public String getTransactionSegmentName()
  {
    return getMetricName();
  }

  public String getTransactionSegmentUri() {
    return "";
  }
}