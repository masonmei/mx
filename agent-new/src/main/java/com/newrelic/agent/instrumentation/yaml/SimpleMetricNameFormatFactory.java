package com.newrelic.agent.instrumentation.yaml;

import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;

public class SimpleMetricNameFormatFactory
  implements MetricNameFormatFactory
{
  private final MetricNameFormat metricNameFormat;

  public SimpleMetricNameFormatFactory(MetricNameFormat metricNameFormat)
  {
    this.metricNameFormat = metricNameFormat;
  }

  public MetricNameFormat getMetricNameFormat(ClassMethodSignature sig, Object object, Object[] args)
  {
    return this.metricNameFormat;
  }
}