package com.newrelic.agent;

import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.JSONStreamAware;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.stats.StatsBase;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class MetricData
  implements JSONStreamAware
{
  private final MetricName metricName;
  private final Integer metricId;
  private final StatsBase stats;

  private MetricData(MetricName metricName, Integer metricId, StatsBase stats)
  {
    this.stats = stats;
    this.metricId = metricId;
    this.metricName = metricName;
  }

  public StatsBase getStats() {
    return this.stats;
  }

  public MetricName getMetricName() {
    return this.metricName;
  }

  public Integer getMetricId() {
    return this.metricId;
  }

  public Object getKey() {
    return this.metricId != null ? this.metricId : this.metricName;
  }

  public String toString()
  {
    return this.metricName.toString();
  }

  public void writeJSONString(Writer writer) throws IOException
  {
    List result = new ArrayList(2);
    if (this.metricId == null)
      result.add(this.metricName);
    else {
      result.add(this.metricId);
    }
    result.add(this.stats);
    JSONArray.writeJSONString(result, writer);
  }

  public static MetricData create(MetricName metricName, StatsBase stats) {
    return create(metricName, null, stats);
  }

  public static MetricData create(MetricName metricName, Integer metricId, StatsBase stats) {
    return new MetricData(metricName, metricId, stats);
  }
}