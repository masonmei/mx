package com.newrelic.agent.jmx.values;

import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.JmxAction;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.jmx.metrics.ServerJmxMetricGenerator;
import java.util.ArrayList;
import java.util.List;

public class Jboss56JmxValues extends JmxFrameworkValues
{
  private static String PREFIX = "jboss.web";
  private static final int METRIC_COUNT = 2;
  private static final List<BaseJmxValue> METRICS = new ArrayList(2);

  private static final JmxMetric ACTIVE_SESSIONS = JmxMetric.create(new String[] { "activeSessions", "ActiveSessions" }, "Active", JmxAction.USE_FIRST_RECORDED_ATT, JmxType.SIMPLE);

  private static final JmxMetric SESSION_ALIVE_TIME = JmxMetric.create(new String[] { "sessionAverageAliveTime", "SessionAverageAliveTime" }, "AverageAliveTime", JmxAction.USE_FIRST_RECORDED_ATT, JmxType.SIMPLE);

  private static final JmxMetric EXPIRED_SESSIONS = JmxMetric.create(new String[] { "expiredSessions", "ExpiredSessions" }, "Expired", JmxAction.USE_FIRST_RECORDED_ATT, JmxType.MONOTONICALLY_INCREASING);

  private static final JmxMetric REJECTED_SESSIONS = JmxMetric.create(new String[] { "rejectedSessions", "RejectedSessions" }, "Rejected", JmxAction.USE_FIRST_RECORDED_ATT, JmxType.MONOTONICALLY_INCREASING);

  private static final JmxMetric CURRENT_MAX_COUNT = ServerJmxMetricGenerator.MAX_THREAD_POOL_COUNT.createMetric("maxThreads");
  private static final JmxMetric CURRENT_ACTIVE_COUNT = ServerJmxMetricGenerator.ACTIVE_THREAD_POOL_COUNT.createMetric("currentThreadsBusy");
  private static final JmxMetric CURRENT_IDLE_COUNT = JmxMetric.create(new String[] { "currentThreadCount", "currentThreadsBusy" }, "Idle", JmxAction.SUBTRACT_ALL_FROM_FIRST, JmxType.SIMPLE);

  public List<BaseJmxValue> getFrameworkMetrics()
  {
    return METRICS;
  }

  public String getPrefix()
  {
    return PREFIX;
  }

  static
  {
    METRICS.add(new BaseJmxValue("jboss.web:type=ThreadPool,name=*", "JmxBuiltIn/ThreadPool/{name}/", new JmxMetric[] { CURRENT_ACTIVE_COUNT, CURRENT_IDLE_COUNT, CURRENT_MAX_COUNT }));

    METRICS.add(new BaseJmxValue("jboss.web:type=Manager,path=*,host=*", "JmxBuiltIn/Session/{path}/", new JmxMetric[] { ACTIVE_SESSIONS, EXPIRED_SESSIONS, REJECTED_SESSIONS, SESSION_ALIVE_TIME }));
  }
}