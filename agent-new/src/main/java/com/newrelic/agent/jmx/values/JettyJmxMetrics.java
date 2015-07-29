package com.newrelic.agent.jmx.values;

import java.util.ArrayList;
import java.util.List;

import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.JmxAction;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.jmx.metrics.ServerJmxMetricGenerator;

public class JettyJmxMetrics extends JmxFrameworkValues {
    private static final int METRIC_COUNT = 1;
    private static final JmxMetric CURRENT_MAX_COUNT =
            ServerJmxMetricGenerator.MAX_THREAD_POOL_COUNT.createMetric("maxThreads");
    private static final JmxMetric CURRENT_IDLE_COUNT =
            ServerJmxMetricGenerator.IDLE_THREAD_POOL_COUNT.createMetric("idleThreads");
    private static final JmxMetric CURRENT_ACTIVE_COUNT = JmxMetric.create(new String[] {"threads", "idleThreads"},
                                                                                  "Active",
                                                                                  JmxAction.SUBTRACT_ALL_FROM_FIRST,
                                                                                  JmxType.SIMPLE);
    private static String PREFIX = "org.eclipse.jetty";
    private static List<BaseJmxValue> METRICS = new ArrayList(1);

    static {
        METRICS.add(new BaseJmxValue("org.eclipse.jetty.util.thread:type=queuedthreadpool,id=*",
                                            "JmxBuiltIn/ThreadPool/{id}/",
                                            new JmxMetric[] {CURRENT_IDLE_COUNT, CURRENT_ACTIVE_COUNT,
                                                                    CURRENT_MAX_COUNT}));
    }

    public List<BaseJmxValue> getFrameworkMetrics() {
        return METRICS;
    }

    public String getPrefix() {
        return PREFIX;
    }
}