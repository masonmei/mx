package com.newrelic.agent.instrumentation.yaml;

import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;

public abstract interface MetricNameFormatFactory
{
  public abstract MetricNameFormat getMetricNameFormat(ClassMethodSignature paramClassMethodSignature,
                                                       Object paramObject, Object[] paramArrayOfObject);
}