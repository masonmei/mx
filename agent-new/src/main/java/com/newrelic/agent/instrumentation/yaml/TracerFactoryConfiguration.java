package com.newrelic.agent.instrumentation.yaml;

import com.newrelic.agent.tracers.metricname.OtherTransSimpleMetricNameFormat;
import java.util.Collections;
import java.util.Map;

public class TracerFactoryConfiguration
{
  private final boolean dispatcher;
  private final MetricNameFormatFactory metricNameFormatFactory;
  private final Map attributes;

  public TracerFactoryConfiguration(String defaultMetricPrefix, boolean pDispatcher, Object metricNameFormat, Map attributes)
  {
    this.attributes = Collections.unmodifiableMap(attributes);
    this.dispatcher = pDispatcher;

    if ((metricNameFormat instanceof String)) {
      this.metricNameFormatFactory = new SimpleMetricNameFormatFactory(new OtherTransSimpleMetricNameFormat(metricNameFormat.toString()));
    }
    else if (null == metricNameFormat)
      this.metricNameFormatFactory = new PointCutFactory.ClassMethodNameFormatDescriptor(defaultMetricPrefix, this.dispatcher);
    else if ((metricNameFormat instanceof MetricNameFormatFactory))
      this.metricNameFormatFactory = ((MetricNameFormatFactory)metricNameFormat);
    else
      throw new RuntimeException("Unsupported metric_name_format value");
  }

  public Map getAttributes()
  {
    return this.attributes;
  }

  public MetricNameFormatFactory getMetricNameFormatFactory() {
    return this.metricNameFormatFactory;
  }

  public boolean isDispatcher() {
    return this.dispatcher;
  }
}