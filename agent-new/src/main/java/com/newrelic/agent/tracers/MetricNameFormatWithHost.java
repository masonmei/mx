package com.newrelic.agent.tracers;

import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.util.Strings;

public class MetricNameFormatWithHost
  implements MetricNameFormat
{
  private final String host;
  private final String metricName;

  private MetricNameFormatWithHost(String host, String library)
  {
    this.host = host;
    this.metricName = Strings.join('/', new String[] { "External", host, library });
  }

  public String getHost() {
    return this.host;
  }

  public String getMetricName()
  {
    return this.metricName;
  }

  public String getTransactionSegmentName()
  {
    return this.metricName;
  }

  public String getTransactionSegmentUri()
  {
    return null;
  }

  public static MetricNameFormatWithHost create(String host, String library) {
    return new MetricNameFormatWithHost(host, library);
  }
}