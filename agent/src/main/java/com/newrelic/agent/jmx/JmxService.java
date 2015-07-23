//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.jmx;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.openmbean.CompositeDataSupport;

import com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.JmxConfig;
import com.newrelic.agent.extension.Extension;
import com.newrelic.agent.jmx.create.JmxGet;
import com.newrelic.agent.jmx.create.JmxInvoke;
import com.newrelic.agent.jmx.create.JmxObjectFactory;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;

public class JmxService extends AbstractService implements HarvestListener {
    private static final int INVOKE_ERROR_COUNT_MAX = 5;
    private static final String J2EE_STATS_ATTRIBUTE_PROCESSOR_CLASS_NAME =
            "com.newrelic.agent.jmx.J2EEStatsAttributeProcessor";
    private static final String WEBSPHERE_STATS_ATTRIBUTE_PROCESSOR_CLASS_NAME =
            "com.newrelic.agent.jmx.WebSphereStatsAttributeProcessor";
    private final boolean enabled;
    private final boolean createMBeanServerIfNecessary;
    private final Set<JmxAttributeProcessor> jmxAttributeProcessors = new HashSet();
    private final JmxObjectFactory jmxMetricFactory;
    private final List<JmxGet> jmxGets = new LinkedList();
    private final List<JmxInvoke> jmxInvokes = new LinkedList();
    private final Queue<JmxFrameworkValues> toBeAdded = new ConcurrentLinkedQueue();
    private final Set<MBeanServer> alwaysIncludeMBeanServers = new CopyOnWriteArraySet();
    private final Set<MBeanServer> toRemoveMBeanServers = new CopyOnWriteArraySet();

    public JmxService() {
        super(JmxService.class.getSimpleName());
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        JmxConfig jmxConfig = config.getJmxConfig();
        this.enabled = jmxConfig.isEnabled();
        this.createMBeanServerIfNecessary = jmxConfig.isCreateMbeanServer();
        this.jmxMetricFactory = JmxObjectFactory.createJmxFactory();
    }

    public List<JmxGet> getConfigurations() {
        return Collections.unmodifiableList(this.jmxGets);
    }

    public void addJmxAttributeProcessor(JmxAttributeProcessor attributeProcessor) {
        this.jmxAttributeProcessors.add(attributeProcessor);
    }

    protected void doStart() {
        if (this.enabled) {
            this.jmxMetricFactory.getStartUpJmxObjects(this.jmxGets, this.jmxInvokes);
            if (this.jmxGets.size() > 0) {
                ServiceFactory.getHarvestService().addHarvestListener(this);
            }

            this.addJmxAttributeProcessor(JmxAttributeProcessorWrapper
                                                  .createInstance("com.newrelic.agent.jmx.J2EEStatsAttributeProcessor"
                                                                          + ""));
            this.addJmxAttributeProcessor(JmxAttributeProcessorWrapper
                                                  .createInstance("com.newrelic.agent.jmx"
                                                                          + ".WebSphereStatsAttributeProcessor"));
        }

    }

    public void createMBeanServerIfNeeded() {
        if (System.getProperty("com.sun.management.jmxremote") == null && MBeanServerFactory
                                                                                  .findMBeanServer((String) null)
                                                                                  .isEmpty()
                    && this.createMBeanServerIfNecessary) {
            try {
                MBeanServerFactory.createMBeanServer();
                this.getLogger().log(Level.FINE, "Created a default MBeanServer");
            } catch (Exception var2) {
                Agent.LOG.severe("The JMX Service was unable to create a default mbean server");
            }
        }

    }

    public final boolean isEnabled() {
        return this.enabled;
    }

    public void addJmxFrameworkValues(JmxFrameworkValues jmxValues) {
        if (this.enabled) {
            this.toBeAdded.add(jmxValues);
        }

    }

    protected void doStop() {
        this.jmxGets.clear();
        this.jmxInvokes.clear();
        this.jmxAttributeProcessors.clear();
    }

    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        if (Agent.LOG.isFinerEnabled()) {
            Agent.LOG.log(Level.FINER, MessageFormat.format("Harvesting JMX metrics for {0}", new Object[] {appName}));
        }

        try {
            this.process(statsEngine);
        } catch (Exception var5) {
            String msg = MessageFormat.format("Unexpected error querying MBeans in JMX service: ",
                                                     new Object[] {var5.toString()});
            this.getLogger().finer(msg);
        }

    }

    public void afterHarvest(String appName) {
    }

    public void setJmxServer(MBeanServer server) {
        if (server != null && !this.alwaysIncludeMBeanServers.contains(server)) {
            Agent.LOG.log(Level.FINE, "JMX Service : MBeanServer of type {0} was added.",
                                 new Object[] {server.getClass().getName()});
            this.alwaysIncludeMBeanServers.add(server);
        }

    }

    public void removeJmxServer(MBeanServer serverToRemove) {
        if (serverToRemove != null) {
            Agent.LOG.log(Level.FINE, "JMX Service : MBeanServer of type {0} was removed.",
                                 new Object[] {serverToRemove.getClass().getName()});
            this.toRemoveMBeanServers.add(serverToRemove);
        }

    }

    private void process(StatsEngine statsEngine, Collection<MBeanServer> srvrList, JmxGet config,
                         Set<String> metricNames) {
        ObjectName name = config.getObjectName();
        if (name != null) {
            Iterator i$ = srvrList.iterator();

            while (i$.hasNext()) {
                MBeanServer server = (MBeanServer) i$.next();

                try {
                    Set e = server.queryMBeans(name, (QueryExp) null);
                    this.getLogger().finer(MessageFormat.format("JMX Service : MBeans query {0}, matches {1}",
                                                                       new Object[] {name, Integer.valueOf(e.size())}));
                    HashMap mbeanToAttValues = new HashMap();
                    Iterator i$1 = e.iterator();

                    while (i$1.hasNext()) {
                        ObjectInstance instance = (ObjectInstance) i$1.next();
                        ObjectName actualName = instance.getObjectName();
                        String rootMetricName = config.getRootMetricName(actualName, server);
                        Collection attributes = config.getAttributes();
                        HashMap values = new HashMap();
                        Iterator i$2 = attributes.iterator();

                        while (i$2.hasNext()) {
                            String attr = (String) i$2.next();

                            try {
                                String[] e1 = attr.split("\\.");
                                Object attrObj = server.getAttribute(instance.getObjectName(), e1[0]);
                                if (attrObj instanceof Attribute) {
                                    this.recordJmxValue(statsEngine, instance, (Attribute) attrObj, rootMetricName,
                                                               attr, values);
                                } else if (attrObj instanceof CompositeDataSupport) {
                                    if (e1.length == 2) {
                                        this.recordJmxValue(statsEngine, instance, new Attribute(attr,
                                                                                                        ((CompositeDataSupport) attrObj)
                                                                                                                .get(e1[1])),
                                                                   rootMetricName, attr, values);
                                    } else {
                                        this.getLogger().fine(MessageFormat
                                                                      .format("Found CompositeDataSupport object for "
                                                                                      + "{0}, but no object attribute"
                                                                                      + " specified, correct syntax "
                                                                                      + "is object.attribute",
                                                                                     new Object[] {attr}));
                                    }
                                } else {
                                    this.recordJmxValue(statsEngine, instance, new Attribute(attr, attrObj),
                                                               rootMetricName, attr, values);
                                }
                            } catch (Exception var20) {
                                this.getLogger().fine(MessageFormat
                                                              .format("An error occurred fetching JMX attribute {0} "
                                                                              + "for metric {1}",
                                                                             new Object[] {attr, name}));
                                this.getLogger().log(Level.FINEST, "JMX error", var20);
                            }
                        }

                        if (!values.isEmpty()) {
                            mbeanToAttValues.put(actualName, values);
                        }
                    }

                    config.recordStats(statsEngine, mbeanToAttValues, server);
                } catch (Exception var21) {
                    this.getLogger().fine(MessageFormat
                                                  .format("An error occurred fetching JMX object matching name {0}",
                                                                 new Object[] {name}));
                    this.getLogger().log(Level.FINEST, "JMX error", var21);
                }
            }

        }
    }

    private void runThroughAndRemoveInvokes(Collection<MBeanServer> srvrList) {
        if (this.jmxInvokes.size() > 0) {
            Iterator invokes = this.jmxInvokes.iterator();

            while (invokes.hasNext()) {
                JmxInvoke current = (JmxInvoke) invokes.next();
                if (this.handleInvoke(srvrList, current)) {
                    invokes.remove();
                } else {
                    current.incrementErrorCount();
                    if (current.getErrorCount() >= 5) {
                        invokes.remove();
                    }
                }
            }
        }

    }

    private boolean handleInvoke(Collection<MBeanServer> srvrList, JmxInvoke invoke) {
        ObjectName name = invoke.getObjectName();
        if (name == null) {
            return true;
        } else {
            boolean isSuccess = false;
            Iterator i$ = srvrList.iterator();

            while (i$.hasNext()) {
                MBeanServer server = (MBeanServer) i$.next();
                if (this.invoke(server, invoke)) {
                    isSuccess = true;
                }
            }

            return isSuccess;
        }
    }

    private boolean invoke(MBeanServer server, JmxInvoke current) {
        try {
            server.invoke(current.getObjectName(), current.getOperationName(), current.getParams(),
                                 current.getSignature());
            this.getLogger().fine(MessageFormat.format("Successfully invoked JMX server for {0}",
                                                              new Object[] {current.getObjectNameString()}));
            return true;
        } catch (Exception var4) {
            this.getLogger().fine(MessageFormat.format("An error occurred invoking JMX server for {0}",
                                                              new Object[] {current.getObjectNameString()}));
            this.getLogger().log(Level.FINEST, "JMX error", var4);
            return false;
        }
    }

    private void recordJmxValue(StatsEngine statsEngine, ObjectInstance instance, Attribute attribute,
                                String rootMetric, String attName, Map<String, Float> values) {
        if (!this.recordCustomJmxValue(statsEngine, instance, attribute, rootMetric, values)) {
            this.recordNonCustomJmxValue(instance, attribute, attName, values);
        }
    }

    private boolean recordCustomJmxValue(StatsEngine statsEngine, ObjectInstance instance, Attribute attribute,
                                         String metricName, Map<String, Float> values) {
        Iterator i$ = this.jmxAttributeProcessors.iterator();

        JmxAttributeProcessor processor;
        do {
            if (!i$.hasNext()) {
                return false;
            }

            processor = (JmxAttributeProcessor) i$.next();
        } while (!processor.process(statsEngine, instance, attribute, metricName, values));

        return true;
    }

    private void recordNonCustomJmxValue(ObjectInstance instance, Attribute attribute, String attName,
                                         Map<String, Float> values) {
        Object value = attribute.getValue();
        Object num = null;
        if (value instanceof Number) {
            num = (Number) value;
        } else if (value instanceof Boolean) {
            num = Integer.valueOf(((Boolean) value).booleanValue() ? 1 : 0);
        } else if (value != null) {
            try {
                num = Float.valueOf(Float.parseFloat(value.toString()));
            } catch (NumberFormatException var8) {
                ;
            }
        }

        if (num != null) {
            this.getLogger()
                    .finer(MessageFormat.format("Recording JMX metric {0} : {1}", new Object[] {attName, value}));
            values.put(attName, Float.valueOf(((Number) num).floatValue()));
        } else if (value == null) {
            this.getLogger().fine(MessageFormat.format("MBean {0} attribute {1} value is null",
                                                              new Object[] {instance.getObjectName(), attName}));
        } else {
            this.getLogger().fine(MessageFormat.format("MBean {0} attribute {1} is not a number ({2}/{3})",
                                                              new Object[] {instance.getObjectName(), attName, value,
                                                                                   value.getClass().getName()}));
        }

    }

    private void process(StatsEngine statsEngine) {
        HashSet metricNames = new HashSet();
        Collection srvrList = this.getServers();
        this.addNewFrameworks();
        this.runThroughAndRemoveInvokes(srvrList);
        Iterator i$ = this.jmxGets.iterator();

        while (i$.hasNext()) {
            JmxGet object = (JmxGet) i$.next();
            this.process(statsEngine, srvrList, object, metricNames);
        }

    }

    private Collection<MBeanServer> getServers() {
        Object srvrList;
        if (this.alwaysIncludeMBeanServers.isEmpty() && this.toRemoveMBeanServers.isEmpty()) {
            srvrList = MBeanServerFactory.findMBeanServer((String) null);
        } else {
            srvrList = Sets.newHashSet(MBeanServerFactory.findMBeanServer((String) null));
            this.getLogger().log(Level.FINEST, "JMX Service : toRemove MBeansServers ({0})",
                                        new Object[] {Integer.valueOf(this.toRemoveMBeanServers.size())});
            ((Collection) srvrList).removeAll(this.toRemoveMBeanServers);
            this.getLogger().log(Level.FINEST, "JMX Service : toAdd MBeansServers ({0})",
                                        new Object[] {Integer.valueOf(this.alwaysIncludeMBeanServers.size())});
            ((Collection) srvrList).addAll(this.alwaysIncludeMBeanServers);
        }

        this.getLogger().log(Level.FINER, "JMX Service : querying MBeansServers ({0})",
                                    new Object[] {Integer.valueOf(((Collection) srvrList).size())});
        return (Collection) srvrList;
    }

    private void addNewFrameworks() {
        for (JmxFrameworkValues framework = (JmxFrameworkValues) this.toBeAdded.poll(); framework != null;
             framework = (JmxFrameworkValues) this.toBeAdded.poll()) {
            this.jmxMetricFactory.convertFramework(framework, this.jmxGets, this.jmxInvokes);
        }

    }

    public void reloadExtensions(Set<Extension> oldExtensions, Set<Extension> extensions) {
        Iterator i$ = this.jmxGets.iterator();

        while (i$.hasNext()) {
            if (oldExtensions.contains(((JmxGet) i$.next()).getOrigin())) {
                i$.remove();
            }
        }

        i$ = extensions.iterator();

        while (i$.hasNext()) {
            Extension newExtension = (Extension) i$.next();
            this.jmxMetricFactory.addExtension(newExtension, this.jmxGets);
        }

    }
}
