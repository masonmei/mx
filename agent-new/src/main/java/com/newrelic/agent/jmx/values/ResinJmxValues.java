package com.newrelic.agent.jmx.values;

import java.util.ArrayList;
import java.util.List;

import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.jmx.metrics.ServerJmxMetricGenerator;

public class ResinJmxValues extends JmxFrameworkValues {
    private static final int METRIC_COUNT = 3;
    private static final List<BaseJmxValue> METRICS = new ArrayList(3);
    private static String PREFIX = "resin";

    static {
        METRICS.add(new BaseJmxValue("resin:type=SessionManager,*", "JmxBuiltIn/Session/{WebApp}/",
                                            new JmxMetric[] {ServerJmxMetricGenerator.SESSION_ACTIVE_COUNT
                                                                     .createMetric("SessionActiveCount"),
                                                                    ServerJmxMetricGenerator.SESSION_EXPIRED_COUNT
                                                                            .createMetric("SessionTimeoutCountTotal")
                                            }));

        METRICS.add(new BaseJmxValue("resin:type=ThreadPool", "JmxBuiltIn/ThreadPool/Resin/",
                                            new JmxMetric[] {ServerJmxMetricGenerator.ACTIVE_THREAD_POOL_COUNT
                                                                     .createMetric("ThreadActiveCount"),
                                                                    ServerJmxMetricGenerator.IDLE_THREAD_POOL_COUNT
                                                                            .createMetric("ThreadIdleCount"),
                                                                    ServerJmxMetricGenerator.MAX_THREAD_POOL_COUNT
                                                                            .createMetric("ThreadMax")}));

        METRICS.add(new BaseJmxValue("resin:type=TransactionManager", "JmxBuiltIn/Transactions/",
                                            new JmxMetric[] {ServerJmxMetricGenerator.TRANS_ROLLED_BACK_COUNT
                                                                     .createMetric("RollbackCountTotal"),
                                                                    ServerJmxMetricGenerator.TRANS_COMMITED_COUNT
                                                                            .createMetric("CommitCountTotal"),
                                                                    ServerJmxMetricGenerator.TRANS_ACTIVE_COUNT
                                                                            .createMetric("TransactionCount")}));
    }

    public List<BaseJmxValue> getFrameworkMetrics() {
        return METRICS;
    }

    public String getPrefix() {
        return PREFIX;
    }
}