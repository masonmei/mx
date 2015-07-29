package com.newrelic.agent.jmx;

import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.JmxApi;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.values.KafkaConsumerJmxValues;
import com.newrelic.agent.jmx.values.KafkaProducerJmxValues;
import com.newrelic.agent.jmx.values.WebSphere7JmxValues;
import com.newrelic.agent.jmx.values.WebSphereJmxValues;
import com.newrelic.agent.service.ServiceFactory;

public class JmxApiImpl implements JmxApi {
    private ConcurrentMap<String, Boolean> addedJmx = Maps.newConcurrentMap();

    public void addJmxMBeanGroup(String name) {
        if (!addedJmx.containsKey(name)) {
            JmxFrameworkValues jmx = getJmxFrameworkValues(name);

            if (null != jmx) {
                Boolean alreadyAdded = (Boolean) addedJmx.putIfAbsent(name, Boolean.TRUE);
                if ((null == alreadyAdded) || (!alreadyAdded.booleanValue())) {
                    ServiceFactory.getJmxService().addJmxFrameworkValues(jmx);
                    Agent.LOG.log(Level.FINE, "Added JMX for {0}", new Object[] {jmx.getPrefix()});
                } else {
                    Agent.LOG.log(Level.FINE, "Skipped JMX. Already added jmx framework: {0}", new Object[] {name});
                }
            } else {
                Agent.LOG.log(Level.FINE, "Skipped JMX. Unknown jmx framework: {0}", new Object[] {name});
            }
        }
    }

    private JmxFrameworkValues getJmxFrameworkValues(String prefixName) {
        if (prefixName != null) {
            if (prefixName.equals(KafkaProducerJmxValues.PREFIX)) {
                return new KafkaProducerJmxValues();
            }
            if (prefixName.equals(KafkaConsumerJmxValues.PREFIX)) {
                return new KafkaConsumerJmxValues();
            }
            if (prefixName.equals(WebSphere7JmxValues.PREFIX)) {
                return new WebSphere7JmxValues();
            }
            if (prefixName.equals(WebSphereJmxValues.PREFIX)) {
                return new WebSphereJmxValues();
            }
        }

        return null;
    }
}