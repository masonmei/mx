package com.newrelic.agent.environment;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.JSONStreamAware;

import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;

public class Environment implements JSONStreamAware, Cloneable {
    public static final String PHYSICAL_CORE_KEY = "Physical Processors";
    private static final String LOGICAL_CORE_KEY = "Logical Processors";
    private static final String TOTAL_MEMORY_MB = "Total Physical Memory (MB)";
    private static final String SOLR_VERSION_KEY = "Solr Version";
    private static final Pattern JSON_WORKAROUND = Pattern.compile("\\\\+$");

    private final List<EnvironmentChangeListener> listeners = Lists.newCopyOnWriteArrayList();
    private final List<List<?>> environmentMap = new ArrayList();
    private volatile AgentIdentity agentIdentity;
    private volatile Integer physicalCoreCount;
    private volatile Float physicalMemoryMB;
    private volatile Object solrVersion;

    public Environment(AgentConfig config, String logFilePath) {
        if (config.isSendEnvironmentInfo()) {
            OperatingSystemMXBean systemMXBean = ManagementFactory.getOperatingSystemMXBean();

            addVariable("Logical Processors", Integer.valueOf(systemMXBean.getAvailableProcessors()));
            addVariable("Arch", systemMXBean.getArch());
            addVariable("OS version", systemMXBean.getVersion());
            addVariable("OS", systemMXBean.getName());
            addVariable("Java vendor", System.getProperty("java.vendor"));
            addVariable("Java VM", System.getProperty("java.vm.name"));
            addVariable("Java VM version", System.getProperty("java.vm.version"));
            addVariable("Java version", System.getProperty("java.version"));
            addVariable("Log path", logFilePath);

            MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();

            addVariable("Heap initial (MB)", Float.valueOf((float) heapMemoryUsage.getInit() / 1048576.0F));
            addVariable("Heap max (MB)", Float.valueOf((float) heapMemoryUsage.getMax() / 1048576.0F));

            if (config.isSendJvmProps()) {
                RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
                List inputArguments = fixInputArguments(runtimeMXBean.getInputArguments());
                environmentMap.add(Arrays.asList(new Object[] {"JVM arguments", inputArguments}));
            }

        }

        String dispatcher = null;
        if (System.getProperty("com.sun.aas.installRoot") != null) {
            dispatcher = "Glassfish";
        } else if (System.getProperty("resin.home") != null) {
            dispatcher = "Resin";
        } else if (System.getProperty("org.apache.geronimo.base.dir") != null) {
            dispatcher = "Apache Geronimo";
        } else if (System.getProperty("weblogic.home") != null) {
            dispatcher = "WebLogic";
        } else if (System.getProperty("wlp.install.dir") != null) {
            dispatcher = "WebSphere Application Server";
        } else if (System.getProperty("was.install.root") != null) {
            dispatcher = "IBM WebSphere Application Server";
        } else if (System.getProperty("jboss.home") != null) {
            dispatcher = "JBoss";
        } else if ((System.getProperty("jboss.home.dir") != null) || (System.getProperty("org.jboss.resolver.warning")
                                                                              != null)
                           || (System.getProperty("jboss.partition.name") != null)) {
            dispatcher = "JBoss Web";
        } else if (System.getProperty("catalina.home") != null) {
            dispatcher = "Apache Tomcat";
        } else if (System.getProperty("jetty.home") != null) {
            dispatcher = "Jetty";
        }

        addVariable("Framework", "java");

        Number appServerPort = (Number) config.getProperty("appserver_port");
        Integer serverPort = null;
        if (appServerPort != null) {
            serverPort = Integer.valueOf(appServerPort.intValue());
        }

        String instanceName = (String) config.getProperty("instance_name");

        agentIdentity = new AgentIdentity(dispatcher, null, serverPort, instanceName);
    }

    private static List<String> fixInputArguments(List<String> args) {
        List fixed = new ArrayList(args.size());
        for (String arg : args) {
            fixed.add(fixString(arg));
        }
        return fixed;
    }

    static String fixString(String arg) {
        Matcher matcher = JSON_WORKAROUND.matcher(arg);
        return matcher.replaceAll("");
    }

    public void addEnvironmentChangeListener(EnvironmentChangeListener listener) {
        listeners.add(listener);
    }

    public void removeEnvironmentChangeListener(EnvironmentChangeListener listener) {
        listeners.remove(listener);
    }

    public void setServerPort(Integer port) {
        AgentIdentity newIdentity = agentIdentity.createWithNewServerPort(port);
        if (newIdentity == null) {
            Agent.LOG.finest("Application server port already set, not changing it to port " + port);
        } else {
            Agent.LOG.finer("Application server port: " + port);

            agentIdentity = newIdentity;
            notifyListenersIdentityChanged();
        }
    }

    public void setInstanceName(String instanceName) {
        AgentIdentity newIdentity = agentIdentity.createWithNewInstanceName(instanceName);
        if (newIdentity == null) {
            Agent.LOG.finest("Instance Name already set, not changing it to " + instanceName);
        } else {
            Agent.LOG.finer("Application server instance name: " + instanceName);

            agentIdentity = newIdentity;
            notifyListenersIdentityChanged();
        }
    }

    private void notifyListenersIdentityChanged() {
        for (EnvironmentChangeListener listener : listeners) {
            listener.agentIdentityChanged(agentIdentity);
        }
    }

    public AgentIdentity getAgentIdentity() {
        return agentIdentity;
    }

    public void addSolrVersion(Object version) {
        if ((solrVersion == null) && (version != null)) {
            Agent.LOG.fine("Setting environment variable: Solr Version: " + version);
            solrVersion = version;

            notifyListenersIdentityChanged();
        } else if (version != null) {
            Agent.LOG.finest("Solr version already set, not changing it to version " + version);
        }
    }

    private void addVariable(String name, Object value) {
        environmentMap.add(Arrays.asList(new Object[] {name, value}));
    }

    public Object getVariable(String name) {
        for (List item : environmentMap) {
            if (name.equals(item.get(0))) {
                return item.get(1);
            }
        }
        return null;
    }

    public void writeJSONString(Writer writer) throws IOException {
        List map = new ArrayList(environmentMap);
        map.add(Arrays.asList(new String[] {"Dispatcher", agentIdentity.getDispatcher()}));
        map.add(Arrays.asList(new Serializable[] {"Physical Processors", physicalCoreCount}));
        map.add(Arrays.asList(new Serializable[] {"Total Physical Memory (MB)", physicalMemoryMB}));
        if (agentIdentity.getDispatcherVersion() != null) {
            map.add(Arrays.asList(new String[] {"Dispatcher Version", agentIdentity.getDispatcherVersion()}));
        }
        if (agentIdentity.getServerPort() != null) {
            map.add(Arrays.asList(new Serializable[] {"Server port", agentIdentity.getServerPort()}));
        }
        if (agentIdentity.getInstanceName() != null) {
            map.add(Arrays.asList(new String[] {"Instance Name", agentIdentity.getInstanceName()}));
        }
        if (solrVersion != null) {
            map.add(Arrays.asList(new Object[] {"Solr Version", solrVersion}));
        }
        JSONArray.writeJSONString(map, writer);
    }

    public void setServerInfo(String dispatcherName, String version) {
        AgentIdentity newIdentity = agentIdentity.createWithNewDispatcher(dispatcherName, version);

        if (newIdentity != null) {
            agentIdentity = newIdentity;
            Agent.LOG.log(Level.FINER, "The dispatcher was set to {0}:{1}.", new Object[] {dispatcherName, version});

            notifyListenersIdentityChanged();
        }
    }

    public void setServerInfo(String serverInfo) {
        Agent.LOG.config("Server Info: " + serverInfo);
        String[] info = serverInfo.split("/");
        if (info.length == 2) {
            setServerInfo(info[0], info[1]);
        }
    }
}