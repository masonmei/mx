package com.newrelic.agent.jmx.values;

import java.util.ArrayList;
import java.util.List;

import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.jmx.metrics.JtaJmxMetricGenerator;
import com.newrelic.agent.jmx.metrics.ServerJmxMetricGenerator;

public class WebSphere7JmxValues extends JmxFrameworkValues {
    private static final int METRIC_COUNT = 2;
    private static final List<BaseJmxValue> METRICS = new ArrayList(2);

    public static String PREFIX = "WebSphere-7";

    static {
        METRICS.add(new BaseJmxValue("WebSphere:type=ThreadPool,name=*,process=*,platform=*,node=*,*",
                                            "JmxBuiltIn/ThreadPool/{name}/",
                                            new JmxMetric[] {ServerJmxMetricGenerator.ACTIVE_THREAD_POOL_COUNT
                                                                     .createMetric("stats.ActiveCount"),
                                                                    ServerJmxMetricGenerator.MAX_THREAD_POOL_COUNT
                                                                            .createMetric("maximumSize")}));

        METRICS.add(new BaseJmxValue("WebSphere:j2eeType=JTAResource,type=TransactionService,name=*,process=*,"
                                             + "platform=*,node=*,*",
                                            "JmxBuiltIn/JTA/{type}/", new JmxMetric[] {JtaJmxMetricGenerator.COMMIT
                                                                                               .createMetric(new String[] {"stats.CommittedCount"}),
                                                                                              JtaJmxMetricGenerator.ROLLBACK
                                                                                                      .createMetric(new String[] {"stats.RolledbackCount"}),
                                                                                              JtaJmxMetricGenerator.TIMEOUT
                                                                                                      .createMetric(new String[] {"stats.GlobalTimeoutCount"})}));

        METRICS.add(new BaseJmxValue("WebSphere:type=SessionManager,name=*,process=*,platform=*,node=*,*",
                                            "JmxBuiltIn/Session/{name}/",
                                            new JmxMetric[] {ServerJmxMetricGenerator.SESSION_ACTIVE_COUNT
                                                                     .createMetric("stats.LiveCount")}));
    }

    public List<BaseJmxValue> getFrameworkMetrics() {
        return METRICS;
    }

    public String getPrefix() {
        return PREFIX;
    }
}