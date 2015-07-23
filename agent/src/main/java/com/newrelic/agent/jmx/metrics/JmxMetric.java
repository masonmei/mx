package com.newrelic.agent.jmx.metrics;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.stats.StatsEngine;

public abstract class JmxMetric {
    private final String attributeMetricName;
    private final String[] attributes;
    private final JmxAction action;

    protected JmxMetric(String pAttribute) {
        this(new String[] {pAttribute}, null, JmxAction.USE_FIRST_ATT);
    }

    protected JmxMetric(String[] pAttributes, String pAttMetricName, JmxAction pAction)
            throws IllegalArgumentException {
        attributes = pAttributes;

        if (pAttMetricName == null) {
            attributeMetricName = pAttributes[0];
        } else {
            attributeMetricName = pAttMetricName;
        }
        action = pAction;
    }

    public static JmxMetric create(String attribute, JmxType type) {
        if (JmxType.MONOTONICALLY_INCREASING.equals(type)) {
            return new MonotonicallyIncreasingJmxMetric(attribute);
        }
        return new SimpleJmxMetric(attribute);
    }

    public static JmxMetric create(String attribute, String attMetricName, JmxType type) {
        if (JmxType.MONOTONICALLY_INCREASING.equals(type)) {
            return new MonotonicallyIncreasingJmxMetric(new String[] {attribute}, attMetricName,
                                                               JmxAction.USE_FIRST_ATT);
        }

        return new SimpleJmxMetric(new String[] {attribute}, attMetricName, JmxAction.USE_FIRST_ATT);
    }

    public static JmxMetric create(String[] attributes, String attMetricName, JmxAction pAction, JmxType type) {
        if ((attributes == null) || (attributes.length == 0)) {
            throw new IllegalArgumentException("A JmxMetric can not be created with zero attributes.");
        }

        if (JmxType.MONOTONICALLY_INCREASING.equals(type)) {
            return new MonotonicallyIncreasingJmxMetric(attributes, attMetricName, pAction);
        }
        return new SimpleJmxMetric(attributes, attMetricName, pAction);
    }

    public abstract void recordStats(StatsEngine paramStatsEngine, String paramString, float paramFloat);

    public abstract JmxType getType();

    public String getAttributeMetricName() {
        return attributeMetricName;
    }

    public String[] getAttributes() {
        return attributes;
    }

    public void applySingleMBean(String rootMetricName, Map<String, Float> inputAttToValues,
                                 Map<String, Float> resultingValues) {
        String fullMetricName = rootMetricName + attributeMetricName;
        try {
            float value = action.performAction(attributes, inputAttToValues);
            Float oldVal = (Float) resultingValues.get(fullMetricName);
            if (oldVal != null) {
                value += oldVal.floatValue();
            }
            resultingValues.put(fullMetricName, Float.valueOf(value));
            Agent.LOG.log(Level.FINER, "Adding Multi Bean: {0} Value: {1}",
                                 new Object[] {fullMetricName, Float.valueOf(value)});
        } catch (IllegalArgumentException e) {
            if (Agent.LOG.isFinerEnabled()) {
                Agent.LOG.log(Level.FINER, MessageFormat.format("JMX Metric {0} not recorded. {1}",
                                                                       new Object[] {fullMetricName, e.getMessage()}));
            }
        }
    }

    public void recordMultMBeanStats(StatsEngine statsEngine, Map<String, Float> metricWithValues) {
        for (Entry current : metricWithValues.entrySet()) {
            if (((String) current.getKey()).length() > 0) {
                recordStats(statsEngine, (String) current.getKey(), ((Float) current.getValue()).floatValue());
                Agent.LOG.log(Level.FINER, "JMX Multi Bean Metric: {0} Value: {1}",
                                     new Object[] {current.getKey(), current.getValue()});
            }
        }
    }

    public void recordSingleMBeanStats(StatsEngine statsEngine, String rootMetricName, Map<String, Float> values) {
        String fullMetricName = rootMetricName + attributeMetricName;
        try {
            float val = action.performAction(attributes, values);
            recordStats(statsEngine, fullMetricName, val);
            Agent.LOG.log(Level.FINER, "JMX Metric: {0} Value: {1}", new Object[] {fullMetricName, Float.valueOf(val)});
        } catch (IllegalArgumentException e) {
            if (Agent.LOG.isFinerEnabled()) {
                Agent.LOG.log(Level.FINER, MessageFormat.format("JMX Metric {0} not recorded. {1}",
                                                                       new Object[] {fullMetricName, e.getMessage()}));
            }
        }
    }
}