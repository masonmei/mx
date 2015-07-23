//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.jmx.create;

import java.util.Iterator;
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

public class JmxSingleMBeanGet extends JmxGet {
    public JmxSingleMBeanGet(String pObjectName, String rootMetricName, String safeName,
                             Map<JmxType, List<String>> pAttributesToType, Extension origin)
            throws MalformedObjectNameException {
        super(pObjectName, rootMetricName, safeName, pAttributesToType, origin);
    }

    public JmxSingleMBeanGet(String pObjectName, String safeName, String pRootMetric, List<JmxMetric> pMetrics,
                             JmxAttributeFilter attributeFilter, JmxMetricModifier modifier)
            throws MalformedObjectNameException {
        super(pObjectName, safeName, pRootMetric, pMetrics, attributeFilter, modifier);
    }

    public void recordStats(StatsEngine statsEngine, Map<ObjectName, Map<String, Float>> resultingMetricToValue,
                            MBeanServer server) {
        Iterator i$ = resultingMetricToValue.entrySet().iterator();

        while (true) {
            Entry currentMBean;
            String actualRootMetricName;
            do {
                do {
                    if (!i$.hasNext()) {
                        return;
                    }

                    currentMBean = (Entry) i$.next();
                    actualRootMetricName = this.getRootMetricName((ObjectName) currentMBean.getKey(), server);
                } while (actualRootMetricName.length() <= 0);
            } while (this.getJmxAttributeFilter() != null && !this.getJmxAttributeFilter()
                                                                      .keepMetric(actualRootMetricName));

            Iterator i$1 = this.getJmxMetrics().iterator();

            while (i$1.hasNext()) {
                JmxMetric current = (JmxMetric) i$1.next();
                current.recordSingleMBeanStats(statsEngine, actualRootMetricName, (Map) currentMBean.getValue());
            }
        }
    }
}
