package com.newrelic.agent.tracers;

import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.util.Strings;

public class ExternalComponentNameFormat
  implements MetricNameFormat
{
  private String metricName;
  private String transactionSegmentName;
  private final String[] operations;
  private final boolean includeOperationInMetric;
  private final String host;
  private final String library;
  private final String transactionSegmentUri;

  public ExternalComponentNameFormat(String host, String library, boolean includeOperationInMetric, String pTransactionSegmentUri, String[] operations)
  {
    this.host = host;
    this.library = library;
    this.operations = operations;
    this.includeOperationInMetric = includeOperationInMetric;
    this.transactionSegmentUri = pTransactionSegmentUri;

    setMetricName();
  }

  public ExternalComponentNameFormat cloneWithNewHost(String hostName) {
    return new ExternalComponentNameFormat(hostName, this.library, this.includeOperationInMetric, this.transactionSegmentUri, this.operations);
  }

  private void setMetricName()
  {
    this.metricName = Strings.join('/', new String[] { "External", this.host, this.library });
    if (this.includeOperationInMetric) {
      this.metricName += fixOperations(this.operations);
      this.transactionSegmentName = this.metricName;
    }
  }

  public String getMetricName()
  {
    return this.metricName;
  }

  public String getTransactionSegmentName()
  {
    if (this.transactionSegmentName == null) {
      this.transactionSegmentName = (this.metricName + fixOperations(this.operations));
    }
    return this.transactionSegmentName;
  }

  private String fixOperations(String[] operations) {
    StringBuilder builder = new StringBuilder();
    for (String operation : operations) {
      if (operation.startsWith("/"))
        builder.append(operation);
      else {
        builder.append('/').append(operation);
      }
    }
    return builder.toString();
  }

  public String getTransactionSegmentUri()
  {
    return this.transactionSegmentUri;
  }

  public static MetricNameFormat create(String host, String library, boolean includeOperationInMetric, String uri, String[] operations)
  {
    return new ExternalComponentNameFormat(host, library, includeOperationInMetric, uri, operations);
  }
}