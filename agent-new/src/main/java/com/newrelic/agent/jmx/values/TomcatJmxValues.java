package com.newrelic.agent.jmx.values;

import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.JmxAction;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.jmx.metrics.ServerJmxMetricGenerator;
import java.util.ArrayList;
import java.util.List;

public class TomcatJmxValues extends JmxFrameworkValues
{
  private static String PREFIX = "Catalina";
  private static final int METRIC_COUNT = 3;
  private static final JmxMetric ACTIVE_SESSIONS = ServerJmxMetricGenerator.SESSION_ACTIVE_COUNT.createMetric("activeSessions");
  private static final JmxMetric EXPIRED_SESSIONS = ServerJmxMetricGenerator.SESSION_EXPIRED_COUNT.createMetric("expiredSessions");
  private static final JmxMetric REJECTED_SESSIONS = ServerJmxMetricGenerator.SESSION_REJECTED_COUNT.createMetric("rejectedSessions");
  private static final JmxMetric SESSION_ALIVE_TIME = ServerJmxMetricGenerator.SESSION_AVG_ALIVE_TIME.createMetric("sessionAverageAliveTime");

  private static final JmxMetric CURRENT_MAX_COUNT = ServerJmxMetricGenerator.MAX_THREAD_POOL_COUNT.createMetric("maxThreads");
  private static final JmxMetric CURRENT_ACTIVE_COUNT = ServerJmxMetricGenerator.ACTIVE_THREAD_POOL_COUNT.createMetric("currentThreadsBusy");
  private static final JmxMetric CURRENT_IDLE_COUNT = JmxMetric.create(new String[] { "currentThreadCount", "currentThreadsBusy" }, "Idle", JmxAction.SUBTRACT_ALL_FROM_FIRST, JmxType.SIMPLE);

  private final List<BaseJmxValue> metrics = new ArrayList(3);

  public TomcatJmxValues() {
    createMetrics("*");
  }

  public TomcatJmxValues(String name) {
    createMetrics(name);
  }

  private void createMetrics(String name)
  {
    this.metrics.add(new BaseJmxValue(name + ":type=Manager,context=*,host=*,*", "JmxBuiltIn/Session/{context}/", new JmxMetric[] { ACTIVE_SESSIONS, EXPIRED_SESSIONS, REJECTED_SESSIONS, SESSION_ALIVE_TIME }));

    this.metrics.add(new BaseJmxValue(name + ":type=Manager,path=*,host=*", "JmxBuiltIn/Session/{path}/", new JmxMetric[] { ACTIVE_SESSIONS, EXPIRED_SESSIONS, REJECTED_SESSIONS, SESSION_ALIVE_TIME }));

    this.metrics.add(new BaseJmxValue(name + ":type=ThreadPool,name=*", "JmxBuiltIn/ThreadPool/{name}/", new JmxMetric[] { CURRENT_ACTIVE_COUNT, CURRENT_IDLE_COUNT, CURRENT_MAX_COUNT }));
  }

  public List<BaseJmxValue> getFrameworkMetrics()
  {
    return this.metrics;
  }

  public String getPrefix()
  {
    return PREFIX;
  }
}