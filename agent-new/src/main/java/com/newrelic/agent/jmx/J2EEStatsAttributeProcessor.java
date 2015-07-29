package com.newrelic.agent.jmx;

import com.newrelic.agent.Agent;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.StatsEngine;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.management.Attribute;
import javax.management.ObjectInstance;
import javax.management.j2ee.statistics.BoundaryStatistic;
import javax.management.j2ee.statistics.CountStatistic;
import javax.management.j2ee.statistics.JCAConnectionPoolStats;
import javax.management.j2ee.statistics.JCAConnectionStats;
import javax.management.j2ee.statistics.JCAStats;
import javax.management.j2ee.statistics.JDBCConnectionPoolStats;
import javax.management.j2ee.statistics.JDBCConnectionStats;
import javax.management.j2ee.statistics.JDBCStats;
import javax.management.j2ee.statistics.JMSConnectionStats;
import javax.management.j2ee.statistics.JMSSessionStats;
import javax.management.j2ee.statistics.JMSStats;
import javax.management.j2ee.statistics.RangeStatistic;
import javax.management.j2ee.statistics.Statistic;
import javax.management.j2ee.statistics.TimeStatistic;

public class J2EEStatsAttributeProcessor extends AbstractStatsAttributeProcessor
{
  public boolean process(StatsEngine statsEngine, ObjectInstance instance, Attribute attribute, String metricName, Map<String, Float> values)
  {
    Object value = attribute.getValue();
    if ((value instanceof javax.management.j2ee.statistics.Stats)) {
      boolean isBuiltInMetric = isBuiltInMetric(metricName);
      if ((value instanceof JDBCStats)) {
        pullJDBCStats(statsEngine, (JDBCStats)value, attribute, metricName, values, isBuiltInMetric);
      } else if ((value instanceof JCAStats)) {
        pullJCAStats(statsEngine, (JCAStats)value, attribute, metricName, values, isBuiltInMetric);
      } else if ((value instanceof JMSStats)) {
        pullJMSStats(statsEngine, (JMSStats)value, attribute, metricName, values, isBuiltInMetric);
      } else {
        javax.management.j2ee.statistics.Stats jmxStats = (javax.management.j2ee.statistics.Stats)value;
        grabBaseStats(statsEngine, jmxStats, attribute, metricName, values, isBuiltInMetric);
      }
      return true;
    }
    Agent.LOG.finer(MessageFormat.format("Attribute value is not a javax.management.j2ee.statistics.Stats: {0}", new Object[] { value.getClass().getName() }));

    return false;
  }

  private static void pullJMSStats(StatsEngine statsEngine, JMSStats jmsStats, Attribute attribute, String metricName, Map<String, Float> values, boolean isBuiltInMetric)
  {
    for (JMSConnectionStats connStats : jmsStats.getConnections())
      for (JMSSessionStats current : connStats.getSessions())
        grabBaseStats(statsEngine, current, attribute, metricName, values, isBuiltInMetric);
  }

  private static void pullJDBCStats(StatsEngine statsEngine, JDBCStats jdbcStats, Attribute attribute, String metricName, Map<String, Float> values, boolean isBuiltInMetric)
  {
    if (jdbcStats.getConnectionPools() != null) {
      for (JDBCConnectionPoolStats current : jdbcStats.getConnectionPools()) {
        grabBaseStats(statsEngine, current, attribute, metricName, values, isBuiltInMetric);
      }
    }
    if (jdbcStats.getConnections() != null)
      for (JDBCConnectionStats current : jdbcStats.getConnections())
        grabBaseStats(statsEngine, current, attribute, metricName, values, isBuiltInMetric);
  }

  private static void pullJCAStats(StatsEngine statsEngine, JCAStats jcaStats, Attribute attribute, String metricName, Map<String, Float> values, boolean isBuiltInMetric)
  {
    if (jcaStats.getConnectionPools() != null) {
      for (JCAConnectionPoolStats current : jcaStats.getConnectionPools()) {
        grabBaseStats(statsEngine, current, attribute, metricName, values, isBuiltInMetric);
      }
    }
    if (jcaStats.getConnections() != null)
      for (JCAConnectionStats current : jcaStats.getConnections())
        grabBaseStats(statsEngine, current, attribute, metricName, values, isBuiltInMetric);
  }

  private static void grabBaseStats(StatsEngine statsEngine, javax.management.j2ee.statistics.Stats jmxStats, Attribute attribute, String metricName, Map<String, Float> values, boolean isBuiltInMetric)
  {
    for (Statistic statistic : jmxStats.getStatistics())
      if (isBuiltInMetric)
      {
        if (addJmxValue(attribute, statistic, values)) {
          break;
        }
      }
      else
        processStatistic(statsEngine, metricName, attribute, statistic);
  }

  static void processStatistic(StatsEngine statsEngine, String metricName, Attribute attribute, Statistic statistic)
  {
    String fullMetricName = metricName + '/' + statistic.getName();
    Agent.LOG.finer(MessageFormat.format("Processing J2EE statistic: {0} class: {1}", new Object[] { statistic.getName(), statistic.getClass().getName() }));

    if ((statistic instanceof CountStatistic)) {
      CountStatistic stat = (CountStatistic)statistic;
      statsEngine.getStats(fullMetricName).recordDataPoint((float)stat.getCount());
    } else if ((statistic instanceof RangeStatistic)) {
      RangeStatistic stat = (RangeStatistic)statistic;
      statsEngine.getStats(fullMetricName).recordDataPoint((float)stat.getCurrent());
    } else if ((statistic instanceof BoundaryStatistic)) {
      BoundaryStatistic stat = (BoundaryStatistic)statistic;
      statsEngine.getStats(fullMetricName).recordDataPoint((float)stat.getLowerBound());
      statsEngine.getStats(fullMetricName).recordDataPoint((float)stat.getUpperBound());
    } else if ((statistic instanceof TimeStatistic)) {
      TimeStatistic stat = (TimeStatistic)statistic;
      TimeUnit unit = getTimeUnit(stat.getUnit());
      statsEngine.getResponseTimeStats(fullMetricName).recordResponseTime((int)stat.getCount(), stat.getTotalTime(), stat.getMinTime(), stat.getMaxTime(), unit);
    }
    else {
      Agent.LOG.log(Level.FINEST, "Not supported: {0}", new Object[] { statistic.getClass().getName() });
    }
    Agent.LOG.finer(MessageFormat.format("Processed J2EE statistic: {0} att: {1}", new Object[] { fullMetricName, statistic.getName() }));
  }

  static boolean addJmxValue(Attribute attribute, Statistic statistic, Map<String, Float> values)
  {
    if (attribute.getName().contains(statistic.getName())) {
      Agent.LOG.finer(MessageFormat.format("Adding J2EE statistic to List: {0} class: {1}", new Object[] { attribute.getName(), statistic.getClass().getName() }));

      if ((statistic instanceof CountStatistic)) {
        CountStatistic stat = (CountStatistic)statistic;
        values.put(attribute.getName(), Float.valueOf((float)stat.getCount()));
        return true;
      }if ((statistic instanceof RangeStatistic)) {
        RangeStatistic stat = (RangeStatistic)statistic;
        values.put(attribute.getName(), Float.valueOf((float)stat.getCurrent()));
        return true;
      }if ((statistic instanceof BoundaryStatistic)) {
        BoundaryStatistic stat = (BoundaryStatistic)statistic;
        values.put(attribute.getName(), Float.valueOf((float)((stat.getLowerBound() + stat.getUpperBound()) / 2L)));
        return true;
      }if ((statistic instanceof TimeStatistic)) {
        TimeStatistic stat = (TimeStatistic)statistic;
        if (stat.getCount() == 0L)
          values.put(attribute.getName(), Float.valueOf(0.0F));
        else {
          values.put(attribute.getName(), Float.valueOf((float)(stat.getTotalTime() / stat.getCount())));
        }
        return true;
      }
      Agent.LOG.finer(MessageFormat.format("Added J2EE statistic: {0}", new Object[] { attribute.getName() }));
    }
    else {
      Agent.LOG.log(Level.FINEST, MessageFormat.format("Ignoring stat {0}. Looking for att name {1}.", new Object[] { statistic.getName(), attribute.getName() }));
    }

    return false;
  }
}