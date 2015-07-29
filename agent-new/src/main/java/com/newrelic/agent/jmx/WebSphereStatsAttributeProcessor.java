//package com.newrelic.agent.jmx;
//
//import com.ibm.websphere.management.statistics.BoundaryStatistic;
//import com.ibm.websphere.management.statistics.CountStatistic;
//import com.ibm.websphere.management.statistics.JCAConnectionPoolStats;
//import com.ibm.websphere.management.statistics.JCAConnectionStats;
//import com.ibm.websphere.management.statistics.JCAStats;
//import com.ibm.websphere.management.statistics.JDBCConnectionPoolStats;
//import com.ibm.websphere.management.statistics.JDBCConnectionStats;
//import com.ibm.websphere.management.statistics.JDBCStats;
//import com.ibm.websphere.management.statistics.JMSConnectionStats;
//import com.ibm.websphere.management.statistics.JMSSessionStats;
//import com.ibm.websphere.management.statistics.JMSStats;
//import com.ibm.websphere.management.statistics.RangeStatistic;
//import com.ibm.websphere.management.statistics.Statistic;
//import com.ibm.websphere.management.statistics.TimeStatistic;
//import com.newrelic.agent.Agent;
//import com.newrelic.agent.logging.IAgentLogger;
//import com.newrelic.agent.stats.ResponseTimeStats;
//import com.newrelic.agent.stats.StatsEngine;
//import java.text.MessageFormat;
//import java.util.Map;
//import java.util.concurrent.TimeUnit;
//import java.util.logging.Level;
//import javax.management.Attribute;
//import javax.management.ObjectInstance;
//
//public class WebSphereStatsAttributeProcessor extends AbstractStatsAttributeProcessor
//{
//  public boolean process(StatsEngine statsEngine, ObjectInstance instance, Attribute attribute, String metricName, Map<String, Float> values)
//  {
//    Object value = attribute.getValue();
//    if ((value instanceof com.ibm.websphere.management.statistics.Stats)) {
//      boolean isBuiltInMetric = isBuiltInMetric(metricName);
//      if ((value instanceof JDBCStats)) {
//        pullJDBCStats(statsEngine, (JDBCStats)value, attribute, metricName, values, isBuiltInMetric);
//      } else if ((value instanceof JCAStats)) {
//        pullJCAStats(statsEngine, (JCAStats)value, attribute, metricName, values, isBuiltInMetric);
//      } else if ((value instanceof JMSStats)) {
//        pullJMSStats(statsEngine, (JMSStats)value, attribute, metricName, values, isBuiltInMetric);
//      } else {
//        com.ibm.websphere.management.statistics.Stats jmxStats = (com.ibm.websphere.management.statistics.Stats)value;
//
//        for (Statistic statistic : jmxStats.getStatistics()) {
//          if (isBuiltInMetric)
//            addJmxValue(attribute, statistic, values);
//          else {
//            processStatistic(statsEngine, metricName, statistic);
//          }
//        }
//      }
//      return true;
//    }
//    return false;
//  }
//
//  private static void pullJMSStats(StatsEngine statsEngine, JMSStats jmsStats, Attribute attribute, String metricName, Map<String, Float> values, boolean isBuiltInMetric)
//  {
//    for (JMSConnectionStats connStats : jmsStats.getConnections())
//      for (JMSSessionStats current : connStats.getSessions())
//        grabBaseStats(statsEngine, current, attribute, metricName, values, isBuiltInMetric);
//  }
//
//  private static void pullJDBCStats(StatsEngine statsEngine, JDBCStats jdbcStats, Attribute attribute, String metricName, Map<String, Float> values, boolean isBuiltInMetric)
//  {
//    if (jdbcStats.getConnectionPools() != null) {
//      for (JDBCConnectionPoolStats current : jdbcStats.getConnectionPools()) {
//        grabBaseStats(statsEngine, current, attribute, metricName, values, isBuiltInMetric);
//      }
//    }
//    if (jdbcStats.getConnections() != null)
//      for (JDBCConnectionStats current : jdbcStats.getConnections())
//        grabBaseStats(statsEngine, current, attribute, metricName, values, isBuiltInMetric);
//  }
//
//  private static void pullJCAStats(StatsEngine statsEngine, JCAStats jcaStats, Attribute attribute, String metricName, Map<String, Float> values, boolean isBuiltInMetric)
//  {
//    if (jcaStats.getConnectionPools() != null) {
//      for (JCAConnectionPoolStats current : jcaStats.getConnectionPools()) {
//        grabBaseStats(statsEngine, current, attribute, metricName, values, isBuiltInMetric);
//      }
//    }
//    if (jcaStats.getConnections() != null)
//      for (JCAConnectionStats current : jcaStats.getConnections())
//        grabBaseStats(statsEngine, current, attribute, metricName, values, isBuiltInMetric);
//  }
//
//  private static void grabBaseStats(StatsEngine statsEngine, com.ibm.websphere.management.statistics.Stats jmxStats, Attribute attribute, String metricName, Map<String, Float> values, boolean isBuiltInMetric)
//  {
//    for (Statistic statistic : jmxStats.getStatistics())
//      if (isBuiltInMetric)
//      {
//        if (addJmxValue(attribute, statistic, values)) {
//          break;
//        }
//      }
//      else
//        processStatistic(statsEngine, metricName, statistic);
//  }
//
//  static void processStatistic(StatsEngine statsEngine, String metricName, Statistic statistic)
//  {
//    String fullMetricName = metricName + '/' + statistic.getName();
//    if ((statistic instanceof CountStatistic)) {
//      CountStatistic stat = (CountStatistic)statistic;
//      statsEngine.getStats(fullMetricName).recordDataPoint((float)stat.getCount());
//    } else if ((statistic instanceof RangeStatistic)) {
//      RangeStatistic stat = (RangeStatistic)statistic;
//      statsEngine.getStats(fullMetricName).recordDataPoint((float)stat.getCurrent());
//    } else if ((statistic instanceof BoundaryStatistic)) {
//      BoundaryStatistic stat = (BoundaryStatistic)statistic;
//      statsEngine.getStats(fullMetricName).recordDataPoint((float)stat.getLowerBound());
//      statsEngine.getStats(fullMetricName).recordDataPoint((float)stat.getUpperBound());
//    } else if ((statistic instanceof TimeStatistic)) {
//      TimeStatistic stat = (TimeStatistic)statistic;
//      TimeUnit unit = getTimeUnit(stat.getUnit());
//      statsEngine.getResponseTimeStats(fullMetricName).recordResponseTime((int)stat.getCount(), stat.getTotalTime(), stat.getMinTime(), stat.getMaxTime(), unit);
//    }
//  }
//
//  static boolean addJmxValue(Attribute attribute, Statistic statistic, Map<String, Float> values)
//  {
//    if (attribute.getName().contains(statistic.getName())) {
//      if ((statistic instanceof CountStatistic)) {
//        CountStatistic stat = (CountStatistic)statistic;
//        values.put(attribute.getName(), Float.valueOf((float)stat.getCount()));
//        return true;
//      }if ((statistic instanceof RangeStatistic)) {
//        RangeStatistic stat = (RangeStatistic)statistic;
//        values.put(attribute.getName(), Float.valueOf((float)stat.getCurrent()));
//        return true;
//      }if ((statistic instanceof BoundaryStatistic)) {
//        BoundaryStatistic stat = (BoundaryStatistic)statistic;
//        values.put(attribute.getName(), Float.valueOf((float)((stat.getLowerBound() + stat.getUpperBound()) / 2L)));
//        return true;
//      }if ((statistic instanceof TimeStatistic)) {
//        TimeStatistic stat = (TimeStatistic)statistic;
//        values.put(attribute.getName(), Float.valueOf((float)(stat.getTotalTime() / stat.getCount())));
//        return true;
//      }
//    } else {
//      Agent.LOG.log(Level.FINEST, MessageFormat.format("Not recording stat {0} because it does not match the attribute name {1}.", new Object[] { statistic.getName(), attribute.getName() }));
//    }
//
//    return false;
//  }
//}