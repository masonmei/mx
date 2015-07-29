package com.newrelic.agent.jmx.values;

import java.util.ArrayList;
import java.util.List;

import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxInit;
import com.newrelic.agent.jmx.metrics.JmxMetric;

@JmxInit
public class JavaLangJmxMetrics extends JmxFrameworkValues {
    private static final int METRIC_COUNT = 2;
    private static final List<BaseJmxValue> METRICS = new ArrayList(2);
    private static final JmxMetric CURRENT_THREAD_COUNT =
            JmxMetric.create("ThreadCount", "Thread Count", JmxType.SIMPLE);
    private static final JmxMetric TOTAL_THREAD_COUNT =
            JmxMetric.create("TotalStartedThreadCount", "TotalStartedCount", JmxType.SIMPLE);
    private static final JmxMetric LOADED_CLASSES = JmxMetric.create("LoadedClassCount", "Loaded", JmxType.SIMPLE);
    private static final JmxMetric UNLOADED_CLASSES =
            JmxMetric.create("UnloadedClassCount", "Unloaded", JmxType.SIMPLE);
    private static String PREFIX = "java.lang";

    static {
        METRICS.add(new BaseJmxValue("java.lang:type=Threading", "JmxBuiltIn/Threads/",
                                            new JmxMetric[] {CURRENT_THREAD_COUNT, TOTAL_THREAD_COUNT}));

        METRICS.add(new BaseJmxValue("java.lang:type=ClassLoading", "JmxBuiltIn/Classes/",
                                            new JmxMetric[] {LOADED_CLASSES, UNLOADED_CLASSES}));
    }

    public List<BaseJmxValue> getFrameworkMetrics() {
        return METRICS;
    }

    public String getPrefix() {
        return PREFIX;
    }
}