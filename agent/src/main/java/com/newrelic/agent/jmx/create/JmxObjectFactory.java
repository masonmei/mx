//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.jmx.create;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.JmxConfig;
import com.newrelic.agent.extension.Extension;
import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.jmx.metrics.BaseJmxInvokeValue;
import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.JMXMetricType;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxInit;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.Annotations;

public class JmxObjectFactory {
    private final Collection<String> disabledJmxFrameworks;

    private JmxObjectFactory() {
        JmxConfig jmxConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getJmxConfig();
        this.disabledJmxFrameworks = jmxConfig.getDisabledJmxFrameworks();
    }

    public static JmxObjectFactory createJmxFactory() {
        return new JmxObjectFactory();
    }

    public void getStartUpJmxObjects(List<JmxGet> jmxGets, List<JmxInvoke> jmxInvokes) {
        this.getStoredJmxObjects(jmxGets, jmxInvokes);
        this.getYmlJmxGets(jmxGets);
    }

    public void convertFramework(JmxFrameworkValues framework, List<JmxGet> jmxGets, List<JmxInvoke> jmxInvokes) {
        if (framework != null) {
            if (this.isDisabled(framework)) {
                Agent.LOG.log(Level.INFO, MessageFormat.format("JMX Metrics for the {0} framework are disabled and "
                                                                       + "therefore are not being loaded.",
                                                                      new Object[] {framework.getPrefix()}));
            } else {
                this.convertToJmxGets(framework, jmxGets);
                this.convertToJmxInvoke(framework, jmxInvokes);
            }
        }

    }

    protected String getSafeObjectName(String pObjectNameString) {
        return pObjectNameString;
    }

    private void createLogAddJmxGet(String pObjectName, String rootMetricName,
                                    Map<JmxType, List<String>> pAttributesToType, List<JmxGet> alreadyAdded,
                                    Extension origin) {
        try {
            JmxSingleMBeanGet e =
                    new JmxSingleMBeanGet(pObjectName, rootMetricName, this.getSafeObjectName(pObjectName),
                                                 pAttributesToType, origin);
            if (e != null) {
                alreadyAdded.add(0, e);
                if (Agent.LOG.isFineEnabled()) {
                    Agent.LOG.log(Level.FINER, MessageFormat.format("Adding JMX config: {0}", new Object[] {e}));
                }
            }
        } catch (Exception var7) {
            Agent.LOG.log(Level.WARNING,
                                 "The JMX configuration is invalid and will not be added. Please check your JMX "
                                         + "configuration file. The object name is " + pObjectName);
        }

    }

    private void createLogAddJmxGet(JMXMetricType type, String pObjectName, String pRootMetricName,
                                    List<JmxMetric> pMetrics, JmxAttributeFilter attributeFilter,
                                    JmxMetricModifier modifier, List<JmxGet> alreadyAdded) {
        try {
            JmxGet e;
            if (type == JMXMetricType.INCREMENT_COUNT_PER_BEAN) {
                e = new JmxSingleMBeanGet(pObjectName, this.getSafeObjectName(pObjectName), pRootMetricName, pMetrics,
                                                 attributeFilter, modifier);
            } else {
                e = new JmxMultiMBeanGet(pObjectName, this.getSafeObjectName(pObjectName), pRootMetricName, pMetrics,
                                                attributeFilter, modifier);
            }

            if (e != null) {
                alreadyAdded.add(0, e);
                if (Agent.LOG.isFineEnabled()) {
                    Agent.LOG.log(Level.FINER, MessageFormat.format("Adding JMX config: {0}", new Object[] {e}));
                }
            }
        } catch (Exception var9) {
            Agent.LOG.log(Level.WARNING,
                                 "The JMX configuration is invalid and will not be added. Please check your JMX "
                                         + "configuration file. The object name is " + pObjectName);
        }

    }

    private void createLogAddJmxInvoke(BaseJmxInvokeValue invoke, List<JmxInvoke> alreadyAdded) {
        try {
            JmxInvoke e =
                    new JmxInvoke(invoke.getObjectNameString(), this.getSafeObjectName(invoke.getObjectNameString()),
                                         invoke.getOperationName(), invoke.getParams(), invoke.getSignature());
            if (e != null) {
                alreadyAdded.add(e);
                if (Agent.LOG.isFineEnabled()) {
                    Agent.LOG.log(Level.FINER, MessageFormat.format("Adding JMX config: {0}", new Object[] {e}));
                }
            }
        } catch (Exception var4) {
            Agent.LOG.log(Level.WARNING,
                                 "The JMX configuration is invalid and will not be added. Please check your JMX "
                                         + "configuration file. The object name is " + invoke.getObjectNameString());
        }

    }

    private void getStoredJmxObjects(List<JmxGet> gets, List<JmxInvoke> invokes) {
        Collection classes =
                Annotations.getAnnotationClassesFromManifest(JmxInit.class, "com/newrelic/agent/jmx/values");
        if (classes != null) {
            Iterator i$ = classes.iterator();

            while (i$.hasNext()) {
                Class clazz = (Class) i$.next();
                this.convertFramework(this.loadJmxFrameworkValues(clazz), gets, invokes);
            }
        }

    }

    private boolean isDisabled(JmxFrameworkValues current) {
        String framework = current.getPrefix();
        return this.disabledJmxFrameworks.contains(framework);
    }

    private void convertToJmxInvoke(JmxFrameworkValues framework, List<JmxInvoke> alreadyAdded) {
        List values = framework.getJmxInvokers();
        if (values != null) {
            Iterator i$ = values.iterator();

            while (i$.hasNext()) {
                BaseJmxInvokeValue value = (BaseJmxInvokeValue) i$.next();
                this.createLogAddJmxInvoke(value, alreadyAdded);
            }
        }

    }

    private void convertToJmxGets(JmxFrameworkValues framework, List<JmxGet> alreadyAdded) {
        List values = framework.getFrameworkMetrics();
        if (values != null) {
            Iterator i$ = values.iterator();

            while (i$.hasNext()) {
                BaseJmxValue value = (BaseJmxValue) i$.next();
                this.createLogAddJmxGet(value.getType(), value.getObjectNameString(), value.getObjectMetricName(),
                                               value.getMetrics(), value.getAttributeFilter(), value.getModifier(),
                                               alreadyAdded);
            }
        }

    }

    private <T extends JmxFrameworkValues> JmxFrameworkValues loadJmxFrameworkValues(Class<T> clazz) {
        try {
            return (JmxFrameworkValues) clazz.getConstructor(new Class[0]).newInstance(new Object[0]);
        } catch (Exception var4) {
            String msg = MessageFormat.format("Unable to create jmx framework values in class {0} : {1}",
                                                     new Object[] {clazz.getName(), var4.toString()});
            Agent.LOG.severe(msg);
            Agent.LOG.log(Level.FINE, msg, var4);
            return null;
        }
    }

    private void getYmlJmxGets(List<JmxGet> alreadyAdded) {
        Iterator i$ = ServiceFactory.getExtensionService().getInternalExtensions().values().iterator();

        while (i$.hasNext()) {
            Extension extension = (Extension) i$.next();
            this.addExtension(extension, alreadyAdded);
        }

    }

    public void addExtension(Extension extension, List<JmxGet> alreadyAdded) {
        if (extension.isEnabled()) {
            this.getStoredJmxGets(extension.getJmxConfig(), alreadyAdded, extension.getName(), extension);
        }

    }

    private void getStoredJmxGets(Collection<JmxConfiguration> configs, List<JmxGet> alreadyAdded, String extensionName,
                                  Extension origin) {
        Iterator i$ = configs.iterator();

        while (true) {
            while (true) {
                while (true) {
                    JmxConfiguration parser;
                    boolean isEnabled;
                    do {
                        if (!i$.hasNext()) {
                            return;
                        }

                        parser = (JmxConfiguration) i$.next();
                        isEnabled = parser.getEnabled();
                    } while (!isEnabled);

                    String objectNameString = parser.getObjectName();
                    if (objectNameString != null && objectNameString.trim().length() != 0) {
                        Map attrs = parser.getAttrs();
                        if (attrs != null && attrs.size() != 0) {
                            this.createLogAddJmxGet(objectNameString, parser.getRootMetricName(), attrs, alreadyAdded,
                                                           origin);
                        } else {
                            Agent.LOG.log(Level.WARNING, MessageFormat
                                                                 .format("Not recording JMX metric with object name "
                                                                                 + "{0} in extension {1} because "
                                                                                 + "there are no attributes.",
                                                                                new Object[] {objectNameString,
                                                                                                     extensionName}));
                        }
                    } else {
                        Agent.LOG.log(Level.WARNING,
                                             "Not recording JMX metric because the object name is null or empty in "
                                                     + "extension "
                                                     + extensionName);
                    }
                }
            }
        }
    }
}
