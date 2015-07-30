package com.newrelic.agent.config;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.ConnectionListener;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.logging.AgentLogManager;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;

public class ConfigServiceImpl extends AbstractService implements ConfigService, ConnectionListener, HarvestListener {
    private static final String SANITIZED_SETTING = "****";
    private final List<AgentConfigListener> listeners2 = new CopyOnWriteArrayList();
    private final File configFile;
    private final Map<String, Object> localSettings;
    private final ConcurrentMap<String, AgentConfig> agentConfigs = new ConcurrentHashMap();
    private final String defaultAppName;
    private long lastModified;
    private volatile AgentConfig defaultAgentConfig;
    private volatile AgentConfig localAgentConfig;

    protected ConfigServiceImpl(AgentConfig config, File configFile, Map<String, Object> localSettings) {
        super(HarvestListener.class.getSimpleName());
        this.configFile = configFile;
        this.localSettings = Collections.unmodifiableMap(localSettings);
        this.localAgentConfig = config;
        this.defaultAgentConfig = this.localAgentConfig;
        this.defaultAppName = this.defaultAgentConfig.getApplicationName();
    }

    public boolean isEnabled() {
        return true;
    }

    protected void doStart() {
        ServiceFactory.getRPMServiceManager().addConnectionListener(this);

        if (this.configFile != null) {
            this.lastModified = this.configFile.lastModified();
            String msg =
                    MessageFormat.format("Configuration file is {0}", new Object[] {this.configFile.getAbsolutePath()});
            getLogger().info(msg);
        }

        Object apdex_t = this.localAgentConfig.getProperty("apdex_t", null);
        if (apdex_t != null) {
            String msg =
                    "The apdex_t setting is obsolete and is ignored! Set the apdex_t value for an application in New "
                            + "Relic UI";
            getLogger().warning(msg);
        }

        Object wait_for_customer_ssl = this.localAgentConfig.getProperty("wait_for_customer_ssl", null);
        if (wait_for_customer_ssl != null) {
            String msg = "The wait_for_customer_ssl setting is obsolete and is ignored!";
            getLogger().warning(msg);
        }

        ServiceFactory.getHarvestService().addHarvestListener(this);
    }

    protected void doStop() {
        ServiceFactory.getRPMServiceManager().removeConnectionListener(this);
        ServiceFactory.getHarvestService().removeHarvestListener(this);
    }

    public void addIAgentConfigListener(AgentConfigListener listener) {
        this.listeners2.add(listener);
    }

    public void removeIAgentConfigListener(AgentConfigListener listener) {
        this.listeners2.remove(listener);
    }

    public Map<String, Object> getLocalSettings() {
        return this.localSettings;
    }

    public Map<String, Object> getSanitizedLocalSettings() {
        Map settings = new HashMap(this.localSettings);
        if (settings.containsKey("proxy_host")) {
            settings.put("proxy_host", "****");
        }
        if (settings.containsKey("proxy_user")) {
            settings.put("proxy_user", "****");
        }
        if (settings.containsKey("proxy_password")) {
            settings.put("proxy_password", "****");
        }
        return settings;
    }

    public AgentConfig getDefaultAgentConfig() {
        return this.defaultAgentConfig;
    }

    public AgentConfig getLocalAgentConfig() {
        return this.localAgentConfig;
    }

    public AgentConfig getAgentConfig(String appName) {
        return getOrCreateAgentConfig(appName);
    }

    public TransactionTracerConfig getTransactionTracerConfig(String appName) {
        return getOrCreateAgentConfig(appName).getTransactionTracerConfig();
    }

    public ErrorCollectorConfig getErrorCollectorConfig(String appName) {
        return getOrCreateAgentConfig(appName).getErrorCollectorConfig();
    }

    public JarCollectorConfig getJarCollectorConfig(String appName) {
        return getOrCreateAgentConfig(appName).getJarCollectorConfig();
    }

    public StripExceptionConfig getStripExceptionConfig(String appName) {
        return getOrCreateAgentConfig(appName).getStripExceptionConfig();
    }

    private void checkConfigFile() throws Exception {
        if (this.configFile.lastModified() == this.lastModified) {
            return;
        }
        Agent.LOG.info("Re-reading New Relic configuration file");
        this.lastModified = this.configFile.lastModified();
        Map settings = AgentConfigHelper.getConfigurationFileSettings(this.configFile);
        AgentConfig agentConfig = createAgentConfig(this.defaultAppName, settings, null);

        Map settings2 = AgentConfigFactory.createMap(this.localSettings);
        settings2.put("audit_mode", Boolean.valueOf(agentConfig.isAuditMode()));
        settings2.put("log_level", agentConfig.getLogLevel());

        this.localAgentConfig = AgentConfigFactory.createAgentConfig(settings2, null);
        AgentLogManager.setLogLevel(agentConfig.getLogLevel());

        notifyListeners2(this.defaultAppName, agentConfig);
    }

    private void notifyListeners2(String appName, AgentConfig agentConfig) {
        for (AgentConfigListener listener : this.listeners2) {
            listener.configChanged(appName, agentConfig);
        }
    }

    private AgentConfig getOrCreateAgentConfig(String appName) {
        AgentConfig agentConfig = findAgentConfig(appName);
        if (agentConfig != null) {
            return agentConfig;
        }
        agentConfig = AgentConfigFactory.createAgentConfig(this.localSettings, null);
        AgentConfig oldAgentConfig = (AgentConfig) this.agentConfigs.putIfAbsent(appName, agentConfig);
        return oldAgentConfig == null ? agentConfig : oldAgentConfig;
    }

    private AgentConfig findAgentConfig(String appName) {
        if ((appName == null) || (appName.equals(this.defaultAppName))) {
            return this.defaultAgentConfig;
        }
        return (AgentConfig) this.agentConfigs.get(appName);
    }

    private AgentConfig createAgentConfig(String appName, Map<String, Object> localSettings,
                                          Map<String, Object> serverData) {
        try {
            return AgentConfigFactory.createAgentConfig(localSettings, serverData);
        } catch (Exception e) {
            String msg = MessageFormat.format("Error configuring application \"{0}\" with server data \"{1}\": {2}",
                                                     new Object[] {appName, serverData, e});

            if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.log(Level.FINER, msg, e);
            } else {
                Agent.LOG.warning(msg);
            }
        }
        return null;
    }

    private void replaceServerConfig(String appName, Map<String, Object> serverData) {
        if (Agent.LOG.isLoggable(Level.FINER)) {
            Agent.LOG.finer(MessageFormat.format("Received New Relic data for {0}: {1}",
                                                        new Object[] {appName, serverData}));
        }

        AgentConfig agentConfig = createAgentConfig(appName, this.localSettings, serverData);
        if (agentConfig == null) {
            return;
        }
        if ((appName == null) || (appName.equals(this.defaultAppName))) {
            this.defaultAgentConfig = agentConfig;
        } else {
            this.agentConfigs.put(appName, agentConfig);
        }
        logIfHighSecurityServerAndLocal(appName, agentConfig, serverData);
        notifyListeners2(appName, agentConfig);
    }

    private void logIfHighSecurityServerAndLocal(String appName, AgentConfig agentConfig,
                                                 Map<String, Object> serverData) {
        if ((agentConfig.isHighSecurity()) && (serverData.containsKey("high_security"))) {
            String msg = MessageFormat.format("The agent is in high security mode for {0}: {1} setting is \"{2}\". {3} "
                                                      + "setting is \"{4}\". Disabling the collection of request "
                                                      + "parameters, message queue parameters, and user attributes.",
                                                     new Object[] {appName, "transaction_tracer.record_sql",
                                                                          agentConfig.getTransactionTracerConfig()
                                                                                  .getRecordSql(), "ssl",
                                                                          Boolean.valueOf(agentConfig.isSSL())});

            Agent.LOG.info(msg);
        }
    }

    public void connected(IRPMService rpmService, Map<String, Object> serverData) {
        String appName = rpmService.getApplicationName();
        replaceServerConfig(appName, serverData);
    }

    public void disconnected(IRPMService rpmService) {
    }

    public void afterHarvest(String appName) {
        if (!appName.equals(this.defaultAppName)) {
            return;
        }
        try {
            checkConfigFile();
        } catch (Throwable t) {
            String msg = MessageFormat.format("Unexpected exception checking for config file changes: {0}",
                                                     new Object[] {t.toString()});

            getLogger().warning(msg);
        }
        ServiceFactory.getClassTransformerService().checkShutdown();
    }

    public void beforeHarvest(String appName, StatsEngine statsEngine) {
    }
}