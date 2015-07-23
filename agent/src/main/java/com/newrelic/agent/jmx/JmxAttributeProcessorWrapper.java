package com.newrelic.agent.jmx;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.management.Attribute;
import javax.management.ObjectInstance;

import com.newrelic.agent.Agent;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.util.CleverClassLoader;

public class JmxAttributeProcessorWrapper implements JmxAttributeProcessor {
    private static final int MAX_SIZE = 100;
    private final JmxAttributeProcessor JMX_ATTRIBUTE_PROCESSOR_NONE = new JmxAttributeProcessorNone();
    private final String jmxAttributeProcessorClassName;
    private final Map<ClassLoader, JmxAttributeProcessor> jmxAttributeProcessorClasses = new HashMap();

    private JmxAttributeProcessorWrapper(String jmxAttributeProcessorClassName) {
        this.jmxAttributeProcessorClassName = jmxAttributeProcessorClassName;
    }

    protected static JmxAttributeProcessor createInstance(String jmxAttributeProcessorClassName) {
        return new JmxAttributeProcessorWrapper(jmxAttributeProcessorClassName);
    }

    public boolean process(StatsEngine statsEngine, ObjectInstance instance, Attribute attribute, String metricName,
                           Map<String, Float> values) {
        Object value = attribute.getValue();
        if ((value == null) || ((value instanceof Number)) || ((value instanceof String))
                    || ((value instanceof Boolean))) {
            return false;
        }
        JmxAttributeProcessor processor = getJmxAttributeProcessor(value);
        if (processor == null) {
            return false;
        }
        return processor.process(statsEngine, instance, attribute, metricName, values);
    }

    private JmxAttributeProcessor getJmxAttributeProcessor(Object attributeValue) {
        ClassLoader cl = attributeValue.getClass().getClassLoader();
        cl = cl == null ? ClassLoader.getSystemClassLoader() : cl;
        JmxAttributeProcessor processor = (JmxAttributeProcessor) jmxAttributeProcessorClasses.get(cl);
        if (processor == null) {
            try {
                CleverClassLoader classLoader = new CleverClassLoader(cl);
                processor = (JmxAttributeProcessor) classLoader.loadClassSpecial(jmxAttributeProcessorClassName)
                                                            .newInstance();
                if (jmxAttributeProcessorClasses.size() > 100) {
                    jmxAttributeProcessorClasses.clear();
                }
                jmxAttributeProcessorClasses.put(cl, processor);
                if (Agent.LOG.isLoggable(Level.FINER)) {
                    String msg = MessageFormat.format("Loaded {0} using class loader {1}",
                                                             new Object[] {jmxAttributeProcessorClassName, cl});

                    Agent.LOG.finer(msg);
                }

            } catch (Throwable t) {
                jmxAttributeProcessorClasses.put(cl, JMX_ATTRIBUTE_PROCESSOR_NONE);
                String msg = MessageFormat.format("Error loading {0} using class loader {1}: {2}",
                                                         new Object[] {jmxAttributeProcessorClassName, cl,
                                                                              t.toString()});

                if (Agent.LOG.isLoggable(Level.FINEST)) {
                    Agent.LOG.log(Level.FINEST, msg, t);
                } else {
                    Agent.LOG.finer(msg);
                }
            }
        }
        return processor;
    }

    private static class JmxAttributeProcessorNone implements JmxAttributeProcessor {
        public boolean process(StatsEngine statsEngine, ObjectInstance instance, Attribute attribute, String metricName,
                               Map<String, Float> values) {
            return false;
        }
    }
}