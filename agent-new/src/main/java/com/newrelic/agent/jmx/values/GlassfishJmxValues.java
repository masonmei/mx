package com.newrelic.agent.jmx.values;

import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.jmx.metrics.BaseJmxInvokeValue;
import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.JmxAction;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.jmx.metrics.ServerJmxMetricGenerator;
import java.util.ArrayList;
import java.util.List;

public class GlassfishJmxValues extends JmxFrameworkValues
{
  private static final int METRIC_COUNT = 3;
  private static final int INVOKE_COUNT = 1;
  private static final List<BaseJmxValue> METRICS = new ArrayList(3);
  private static final List<BaseJmxInvokeValue> INVOKERS = new ArrayList(1);

  private static String PREFIX = "amx";

  private static final JmxMetric CURRENT_IDLE_COUNT = JmxMetric.create(new String[] { "currentthreadcount.count", "currentthreadsbusy.count" }, "Idle", JmxAction.SUBTRACT_ALL_FROM_FIRST, JmxType.SIMPLE);

  public List<BaseJmxValue> getFrameworkMetrics()
  {
    return METRICS;
  }

  public String getPrefix()
  {
    return PREFIX;
  }

  public List<BaseJmxInvokeValue> getJmxInvokers()
  {
    return INVOKERS;
  }

  static
  {
    METRICS.add(new BaseJmxValue("amx:type=thread-pool-mon,pp=*,name=*", "JmxBuiltIn/ThreadPool/{name}/", new JmxMetric[] { ServerJmxMetricGenerator.ACTIVE_THREAD_POOL_COUNT.createMetric("currentthreadsbusy.count"), ServerJmxMetricGenerator.MAX_THREAD_POOL_COUNT.createMetric("maxthreads.count"), CURRENT_IDLE_COUNT }));

    METRICS.add(new BaseJmxValue("amx:type=session-mon,pp=*,name=*", "JmxBuiltIn/Session/{name}/", new JmxMetric[] { ServerJmxMetricGenerator.SESSION_ACTIVE_COUNT.createMetric("activesessionscurrent.current"), ServerJmxMetricGenerator.SESSION_EXPIRED_COUNT.createMetric("expiredsessionstotal.count"), ServerJmxMetricGenerator.SESSION_REJECTED_COUNT.createMetric("rejectedsessionstotal.count") }));

    METRICS.add(new BaseJmxValue("amx:type=transaction-service-mon,pp=*,name=*", "JmxBuiltIn/Transactions/", new JmxMetric[] { ServerJmxMetricGenerator.TRANS_ACTIVE_COUNT.createMetric("activecount.count"), ServerJmxMetricGenerator.TRANS_COMMITED_COUNT.createMetric("committedcount.count"), ServerJmxMetricGenerator.TRANS_ROLLED_BACK_COUNT.createMetric("rolledbackcount.count") }));

    INVOKERS.add(new BaseJmxInvokeValue("amx-support:type=boot-amx", "bootAMX", new Object[0], new String[0]));
  }
}