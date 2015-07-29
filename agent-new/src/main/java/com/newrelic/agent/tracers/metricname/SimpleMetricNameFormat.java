package com.newrelic.agent.tracers.metricname;

public class SimpleMetricNameFormat
  implements MetricNameFormat
{
  private final String metricName;
  private final String transactionSegmentName;
  private final String transactionSegmentUri;

  public SimpleMetricNameFormat(String metricName)
  {
    this(metricName, metricName, null);
  }

  public SimpleMetricNameFormat(String metricName, String transactionSegmentName) {
    this(metricName, transactionSegmentName, null);
  }

  public SimpleMetricNameFormat(String metricName, String transactionSegmentName, String transactionSegmentUri) {
    this.metricName = metricName;
    this.transactionSegmentName = transactionSegmentName;
    this.transactionSegmentUri = transactionSegmentUri;
  }

  public final String getMetricName()
  {
    return this.metricName;
  }

  public String getTransactionSegmentName()
  {
    return this.transactionSegmentName;
  }

  public String getTransactionSegmentUri()
  {
    return this.transactionSegmentUri;
  }
}