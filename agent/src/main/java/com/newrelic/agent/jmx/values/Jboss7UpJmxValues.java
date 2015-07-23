package com.newrelic.agent.jmx.values;

import java.util.ArrayList;
import java.util.List;

import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.JmxAction;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.jmx.metrics.ServerJmxMetricGenerator;

public class Jboss7UpJmxValues extends JmxFrameworkValues {
    private static final int METRIC_COUNT = 1;
    private static final List<BaseJmxValue> METRICS = new ArrayList(1);

    private static String PREFIX = "jboss.as";

    static {
        METRICS.add(new BaseJmxValue("jboss.as:subsystem=transactions", "JmxBuiltIn/Transactions/",
                                            new JmxMetric[] {ServerJmxMetricGenerator.TRANS_ROLLED_BACK_COUNT
                                                                     .createMetric("numberOfAbortedTransactions"),
                                                                    ServerJmxMetricGenerator.TRANS_COMMITED_COUNT
                                                                            .createMetric
                                                                                     ("numberOfCommittedTransactions"),
                                                                    ServerJmxMetricGenerator.TRANS_ACTIVE_COUNT
                                                                            .createMetric
                                                                                     ("numberOfInflightTransactions"),
                                                                    ServerJmxMetricGenerator.TRANS_NESTED_COUNT
                                                                            .createMetric("numberOfNestedTransactions"),
                                                                    JmxMetric
                                                                            .create(new String[]
                                                                                            {"numberOfTransactions",
                                                                                                         "numberOfNestedTransactions"},
                                                                                           "Created/Top Level",
                                                                                           JmxAction
                                                                                                   .SUBTRACT_ALL_FROM_FIRST,
                                                                                           JmxType.MONOTONICALLY_INCREASING)}));
    }

    public List<BaseJmxValue> getFrameworkMetrics() {
        return METRICS;
    }

    public String getPrefix() {
        return PREFIX;
    }
}