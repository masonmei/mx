package com.newrelic.agent.jmx.create;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.newrelic.agent.extension.Extension;
import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.stats.StatsEngine;

public class JmxMultiMBeanGet extends JmxGet {
    public JmxMultiMBeanGet(String pObjectName, String rootMetricName, String safeName,
                            Map<JmxType, List<String>> pAttributesToType, Extension origin)
            throws MalformedObjectNameException {
        super(pObjectName, rootMetricName, safeName, pAttributesToType, origin);
    }

    public JmxMultiMBeanGet(String pObjectName, String safeName, String pRootMetric, List<JmxMetric> pMetrics,
                            JmxAttributeFilter attributeFilter, JmxMetricModifier modifier)
            throws MalformedObjectNameException {
        super(pObjectName, safeName, pRootMetric, pMetrics, attributeFilter, modifier);
    }

    public void recordStats(StatsEngine statsEngine, Map<ObjectName, Map<String, Float>> resultingMetricToValue,
                            MBeanServer server) {
        Map rootMetricNames = new HashMap();
        for (JmxMetric currentMetric : getJmxMetrics()) {
            Map mbeansWithValues = new HashMap();
            for (Entry currentMBean : resultingMetricToValue.entrySet()) {
                String actualRootMetricName = (String) rootMetricNames.get(currentMBean.getKey());
                if (actualRootMetricName == null) {
                    actualRootMetricName = getRootMetricName((ObjectName) currentMBean.getKey(), server);
                }
                currentMetric.applySingleMBean(actualRootMetricName, (Map) currentMBean.getValue(), mbeansWithValues);
            }
            currentMetric.recordMultMBeanStats(statsEngine, mbeansWithValues);
        }
    }
}