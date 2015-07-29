package com.newrelic.agent;

import java.lang.management.ManagementFactory;
import java.net.ConnectException;
import java.rmi.UnexpectedException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.newrelic.deps.org.apache.http.conn.HttpHostConnectException;
import com.newrelic.deps.org.json.simple.JSONObject;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.AgentJarHelper;
import com.newrelic.agent.config.BrowserMonitoringConfig;
import com.newrelic.agent.config.Hostname;
import com.newrelic.agent.config.SystemPropertyFactory;
import com.newrelic.agent.environment.AgentIdentity;
import com.newrelic.agent.environment.Environment;
import com.newrelic.agent.environment.EnvironmentChangeListener;
import com.newrelic.agent.errors.ErrorService;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.metric.MetricIdRegistry;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.normalization.Normalizer;
import com.newrelic.agent.profile.IProfile;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.CustomInsightsEvent;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.module.Jar;
import com.newrelic.agent.sql.SqlTrace;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.trace.TransactionTrace;
import com.newrelic.agent.transport.DataSender;
import com.newrelic.agent.transport.DataSenderFactory;
import com.newrelic.agent.transport.HttpError;
import com.newrelic.agent.utilization.UtilizationData;

public class RPMService extends AbstractService implements IRPMService, EnvironmentChangeListener, AgentConfigListener {
    public static final String COLLECT_TRACES_KEY = "collect_traces";
    public static final String COLLECT_ERRORS_KEY = "collect_errors";
    public static final String DATA_REPORT_PERIOD_KEY = "data_report_period";
    public static final String TRANSACTION_NAME_NAMING_SCHEME_KEY = "transaction_name.naming_scheme";
    public static final String SUPPORTABILITY_METRIC_HARVEST_INTERVAL = "Supportability/MetricHarvest/interval";
    public static final String SUPPORTABILITY_METRIC_HARVEST_TRANSMIT = "Supportability/MetricHarvest/transmit";
    public static final String SUPPORTABILITY_METRIC_HARVEST_COUNT = "Supportability/MetricHarvest/count";
    public static final String AGENT_METRICS_COUNT = "Agent/Metrics/Count";
    public static final int DEFAULT_REQUEST_TIMEOUT_IN_SECONDS = 120;
    public static final String FRAMEWORK_TRANSACTION_NAMING_SCHEME = "framework";
    private static final int MESSAGE_LIMIT_PER_PERIOD = 20;
    private static final int LOG_MESSAGE_COUNT = 5;
    private static final String INTERMITTENT_503_MESSAGE = "Server returned HTTP response code: 503";
    private final String host;
    private final int port;
    private final ErrorService errorService;
    private final String appName;
    private final List<String> appNames;
    private final ConnectionListener connectionListener;
    private final boolean isMainApp;
    private final DataSender dataSender;
    private final MetricIdRegistry metricIdRegistry = new MetricIdRegistry();
    private final AtomicInteger last503Error = new AtomicInteger(0);
    private final AtomicInteger retryCount = new AtomicInteger(0);
    private volatile boolean connected = false;
    private long lastReportTime;
    private volatile boolean hasEverConnected = false;
    private volatile String transactionNamingScheme;
    private long connectionTimestamp = 0L;

    public RPMService(List<String> appNames, ConnectionListener connectionListener) {
        super(RPMService.class.getSimpleName() + "/" + (String) appNames.get(0));
        appName = ((String) appNames.get(0)).intern();
        AgentConfig config = ServiceFactory.getConfigService().getAgentConfig(appName);
        dataSender = DataSenderFactory.create(config);

        this.appNames = appNames;
        this.connectionListener = connectionListener;

        lastReportTime = System.currentTimeMillis();

        errorService = new ErrorService(appName);

        host = config.getHost();

        port = config.getPort();
        isMainApp = appName.equals(config.getApplicationName());
    }

    public boolean isEnabled() {
        return true;
    }

    protected void doStart() {
        connect();
        ServiceFactory.getEnvironmentService().getEnvironment().addEnvironmentChangeListener(this);
        ServiceFactory.getConfigService().addIAgentConfigListener(this);
        ServiceFactory.getServiceManager().getCircuitBreakerService().addRPMService(this);
    }

    private Boolean getAndLogHighSecurity(AgentConfig config) {
        boolean isHighSec = config.isHighSecurity();
        if (isHighSec) {
            Agent.LOG.log(Level.INFO, "High security is configured locally for application {0}.",
                                 new Object[] {appName});
        }
        return Boolean.valueOf(isHighSec);
    }

    protected Map<String, Object> getStartOptions() {
        AgentConfig agentConfig = ServiceFactory.getConfigService().getAgentConfig(appName);

        int pid = ServiceFactory.getEnvironmentService().getProcessPID();
        Map options = new HashMap();

        options.put("pid", Integer.valueOf(pid));
        String language = agentConfig.getLanguage();
        options.put("language", language);
        String defaultHost = Hostname.getHostname(Agent.LOG, agentConfig);
        options.put("host", defaultHost);
        options.put("display_host", Hostname.getDisplayHostname(Agent.LOG, agentConfig, defaultHost, appName));

        options.put("high_security", getAndLogHighSecurity(agentConfig));

        Environment environment = ServiceFactory.getEnvironmentService().getEnvironment();
        options.put("environment", environment);
        if (((Boolean) agentConfig.getProperty("send_environment_info", Boolean.valueOf(true))).booleanValue()) {
            options.put("settings", getSettings());
        }

        UtilizationData utilizationData = ServiceFactory.getUtilizationService().updateUtilizationData();
        options.put("utilization", utilizationData.map());

        String instanceName = environment.getAgentIdentity().getInstanceName();
        if (instanceName != null) {
            options.put("instance_name", instanceName);
        }

        options.put("agent_version", Agent.getVersion());
        options.put("app_name", appNames);

        StringBuilder identifier = new StringBuilder(language);
        identifier.append(':').append(appName);
        Integer serverPort = environment.getAgentIdentity().getServerPort();
        if (serverPort != null) {
            identifier.append(':').append(serverPort);
        }
        options.put("identifier", identifier.toString());

        options.put("labels", agentConfig.getLabelsConfig());

        return options;
    }

    private Map<String, Object> getSettings() {
        Map localSettings = ServiceFactory.getConfigService().getSanitizedLocalSettings();
        Map settings = new HashMap(localSettings);
        Map props = SystemPropertyFactory.getSystemPropertyProvider().getNewRelicSystemProperties();
        if (!props.isEmpty()) {
            settings.put("system", props);
        }

        BrowserMonitoringConfig browserConfig =
                ServiceFactory.getConfigService().getAgentConfig(appName).getBrowserMonitoringConfig();
        settings.put("browser_monitoring.loader", browserConfig.getLoaderType());

        settings.put("browser_monitoring.debug", Boolean.valueOf(browserConfig.isDebug()));

        String buildDate = AgentJarHelper.getBuildDate();
        if (buildDate != null) {
            settings.put("build_date", buildDate);
        }
        settings.put("services", ServiceFactory.getServicesConfiguration());
        return settings;
    }

    public synchronized Map<String, Object> launch() throws Exception {
        if (isConnected()) {
            return null;
        }

        Map data = null;
        try {
            data = dataSender.connect(getStartOptions());
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
        Agent.LOG.log(Level.FINER, "Connection response : {0}", new Object[] {data});
        List requiredParams = Arrays.asList(new String[] {"collect_errors", "collect_traces", "data_report_period"});
        if (!data.keySet().containsAll(requiredParams)) {
            throw new UnexpectedException(MessageFormat.format("Missing the following connection parameters",
                                                                      new Object[] {Boolean.valueOf(requiredParams
                                                                                                            .removeAll(data.keySet()))}));
        }

        Agent.LOG.log(Level.INFO, "Agent {0} connected to {1}", new Object[] {toString(), getHostString()});

        transactionNamingScheme = ((String) data.get("transaction_name.naming_scheme"));
        if (transactionNamingScheme != null) {
            transactionNamingScheme = transactionNamingScheme.intern();
            Agent.LOG.log(Level.INFO, "Setting: {0} to: {1}",
                                 new Object[] {"transaction_name.naming_scheme", transactionNamingScheme});
        }

        try {
            logCollectorMessages(data);
        } catch (Exception ex) {
            Agent.LOG.log(Level.FINEST, ex, "Error processing collector connect messages", new Object[0]);
        }

        connectionTimestamp = System.nanoTime();
        connected = true;
        hasEverConnected = true;
        if (connectionListener != null) {
            connectionListener.connected(this, data);
        }

        return data;
    }

    private void logCollectorMessages(Map<String, Object> data) {
        List<Map> messages = (List<Map>) data.get("messages");
        if (messages != null) {
            for (Map message : messages) {
                String level = (String) message.get("level");
                String text = (String) message.get("message");

                Agent.LOG.log(Level.parse(level), text);
            }
        }
    }

    public String getTransactionNamingScheme() {
        return transactionNamingScheme;
    }

    private void logForceDisconnectException(ForceDisconnectException e) {
        Agent.LOG.log(Level.INFO, "Received a ForceDisconnectException: {0}", new Object[] {e.toString()});
    }

    private void shutdownAsync() {
        ServiceFactory.getAgent().shutdownAsync();
    }

    private void logForceRestartException(ForceRestartException e) {
        Agent.LOG.log(Level.INFO, "Received a ForceRestartException: {0}", new Object[] {e.toString()});
    }

    private void reconnectSync() throws Exception {
        disconnect();
        launch();
    }

    private void reconnectAsync() {
        disconnect();
        ServiceFactory.getRPMConnectionService().connectImmediate(this);
    }

    private void disconnect() {
        connected = false;
        metricIdRegistry.clear();
    }

    public synchronized void reconnect() {
        Agent.LOG.log(Level.INFO, "{0} is reconnecting", new Object[] {getApplicationName()});
        try {
            shutdown();
        } catch (Exception e) {
        } finally {
            reconnectAsync();
        }
    }

    public String getHostString() {
        return MessageFormat.format("{0}:{1}", new Object[] {host, Integer.toString(port)});
    }

    public String toString() {
        StringBuilder builder = new StringBuilder(ManagementFactory.getRuntimeMXBean().getName());
        builder.append('/').append(appName);
        return builder.toString();
    }

    private void sendErrorData(List<TracedError> errors) {
        Agent.LOG.log(Level.FINE, "Sending {0} error(s)", new Object[] {Integer.valueOf(errors.size())});
        try {
            dataSender.sendErrorData(errors);
        } catch (IgnoreSilentlyException e) {
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
        } catch (Exception e) {
            String msg = MessageFormat.format("Error sending error data to New Relic: {0}", new Object[] {e});
            if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.log(Level.FINER, msg, e);
            } else {
                Agent.LOG.warning(msg);
            }
        }
    }

    public List<Long> sendProfileData(List<IProfile> profiles) throws Exception {
        Agent.LOG.log(Level.INFO, "Sending {0} profile(s)", new Object[] {Integer.valueOf(profiles.size())});
        try {
            return sendProfileDataSyncRestart(profiles);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    private List<Long> sendProfileDataSyncRestart(List<IProfile> profiles) throws Exception {
        try {
            return dataSender.sendProfileData(profiles);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
        }
        return dataSender.sendProfileData(profiles);
    }

    public void sendModules(List<Jar> pJarsToSend) throws Exception {
        Agent.LOG.log(Level.FINE, "Sending {0} module(s)", new Object[] {Integer.valueOf(pJarsToSend.size())});
        try {
            sendModulesSyncRestart(pJarsToSend);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    private void sendModulesSyncRestart(List<Jar> pJarsToSend) throws Exception {
        try {
            dataSender.sendModules(pJarsToSend);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
            dataSender.sendModules(pJarsToSend);
        }
    }

    public void sendAnalyticsEvents(Collection<TransactionEvent> events) throws Exception {
        Agent.LOG.log(Level.FINE, "Sending {0} analytics event(s)", new Object[] {Integer.valueOf(events.size())});
        try {
            sendAnalyticsEventsSyncRestart(events);
        } catch (HttpError e) {
            if ((e.getStatusCode() != 413) && (e.getStatusCode() != 415)) {
                throw e;
            }
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    private void sendAnalyticsEventsSyncRestart(Collection<TransactionEvent> events) throws Exception {
        try {
            dataSender.sendAnalyticsEvents(events);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
            dataSender.sendAnalyticsEvents(events);
        }
    }

    public void sendCustomAnalyticsEvents(Collection<CustomInsightsEvent> events) throws Exception {
        Agent.LOG.log(Level.FINE, "Sending {0} analytics event(s)", new Object[] {Integer.valueOf(events.size())});
        try {
            sendCustomAnalyticsEventsSyncRestart(events);
        } catch (HttpError e) {
            if ((e.getStatusCode() != 413) && (e.getStatusCode() != 415)) {
                throw e;
            }
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    private void sendCustomAnalyticsEventsSyncRestart(Collection<CustomInsightsEvent> events) throws Exception {
        try {
            dataSender.sendCustomAnalyticsEvents(events);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
            dataSender.sendCustomAnalyticsEvents(events);
        }
    }

    public void sendSqlTraceData(List<SqlTrace> sqlTraces) throws Exception {
        Agent.LOG.log(Level.FINE, "Sending {0} sql trace(s)", new Object[] {Integer.valueOf(sqlTraces.size())});
        try {
            sendSqlTraceDataSyncRestart(sqlTraces);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    private void sendSqlTraceDataSyncRestart(List<SqlTrace> sqlTraces) throws Exception {
        try {
            dataSender.sendSqlTraceData(sqlTraces);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
            dataSender.sendSqlTraceData(sqlTraces);
        }
    }

    public void sendTransactionTraceData(List<TransactionTrace> traces) throws Exception {
        Agent.LOG.log(Level.FINE, "Sending {0} trace(s)", new Object[] {Integer.valueOf(traces.size())});
        try {
            sendTransactionTraceDataSyncRestart(traces);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    private void sendTransactionTraceDataSyncRestart(List<TransactionTrace> traces) throws Exception {
        try {
            dataSender.sendTransactionTraceData(traces);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
            dataSender.sendTransactionTraceData(traces);
        }
    }

    public ErrorService getErrorService() {
        return errorService;
    }

    public String getApplicationName() {
        return appName;
    }

    public boolean isMainApp() {
        return isMainApp;
    }

    public synchronized void shutdown() throws Exception {
        try {
            if (isConnected()) {
                dataSender.shutdown(System.currentTimeMillis());
            }
        } finally {
            disconnect();
        }
    }

    public List<List<?>> getAgentCommands() throws Exception {
        try {
            return getAgentCommandsSyncRestart();
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    public Collection<?> getXRaySessionInfo(Collection<Long> newIds) throws Exception {
        try {
            return dataSender.getXRayParameters(newIds);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    private List<List<?>> getAgentCommandsSyncRestart() throws Exception {
        try {
            return dataSender.getAgentCommands();
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
        }
        return dataSender.getAgentCommands();
    }

    public void sendCommandResults(Map<Long, Object> commandResults) throws Exception {
        try {
            sendCommandResultsSyncRestart(commandResults);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    private void sendCommandResultsSyncRestart(Map<Long, Object> commandResults) throws Exception {
        try {
            dataSender.sendCommandResults(commandResults);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
            dataSender.sendCommandResults(commandResults);
        }
    }

    public void queuePingCommand() throws Exception {
        try {
            queuePingCommandSyncRestart();
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectAsync();
            throw e;
        } catch (ForceDisconnectException e) {
            logForceDisconnectException(e);
            shutdownAsync();
            throw e;
        }
    }

    private void queuePingCommandSyncRestart() throws Exception {
        try {
            dataSender.queuePingCommand();
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
            dataSender.queuePingCommand();
        }
    }

    public void connect() {
        ServiceFactory.getRPMConnectionService().connect(this);
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean hasEverConnected() {
        return hasEverConnected;
    }

    public void harvest(StatsEngine statsEngine) {
        List errors = errorService.harvest(this, statsEngine);

        if (!isConnected()) {
            try {
                Agent.LOG.fine("Trying to re-establish connection to New Relic.");
                launch();
            } catch (Exception e) {
                Agent.LOG.fine("Problem trying to re-establish connection to New Relic: " + e.getMessage());
            }
        }

        if (isConnected()) {
            boolean retry = false;
            if (metricIdRegistry.getSize() > 1000) {
                statsEngine.getStats("Agent/Metrics/Count").setCallCount(metricIdRegistry.getSize());
            }
            Normalizer metricNormalizer = ServiceFactory.getNormalizationService().getMetricNormalizer(appName);
            List data = statsEngine.getMetricData(metricNormalizer, metricIdRegistry);
            long startTime = System.nanoTime();
            long reportInterval = 0L;
            try {
                long now = System.currentTimeMillis();
                List responseList = sendMetricDataSyncRestart(lastReportTime, now, data);
                reportInterval = now - lastReportTime;
                lastReportTime = now;
                registerMetricIds(responseList);
                last503Error.set(0);

                if (retryCount.get() > 0) {
                    Agent.LOG.log(Level.INFO, "Successfully reconnected to the New Relic data service.");
                }
                Agent.LOG.log(Level.FINE, "Reported {0} timeslices for {1}",
                                     new Object[] {Integer.valueOf(data.size()), getApplicationName()});

                sendErrorData(errors);
            } catch (InternalLimitExceeded e) {
                Agent.LOG.log(Level.SEVERE, "The metric data post was too large.  {0} timeslices will not be resent",
                                     new Object[] {Integer.valueOf(data.size())});
            } catch (MetricDataException e) {
                Agent.LOG.log(Level.SEVERE,
                                     "An invalid response was received while sending metric data. This data will not "
                                             + "be resent.");

                Agent.LOG.log(Level.FINEST, e, e.toString(), new Object[0]);
            } catch (HttpError e) {
                retry = e.isRetryableError();

                if (503 == e.getStatusCode()) {
                    handle503Error(e);
                } else if (retry) {
                    Agent.LOG.log(Level.INFO, "An error occurred posting metric data - {0}.  This data will be resent "
                                                      + "later.", new Object[] {e.getMessage()});
                } else {
                    Agent.LOG.log(Level.SEVERE,
                                         "An error occurred posting metric data - {0}.  {1} timeslices will not be "
                                                 + "resent.",
                                         new Object[] {e.getMessage(), Integer.valueOf(data.size())});
                }
            } catch (ForceRestartException e) {
                logForceRestartException(e);
                reconnectAsync();
                retry = true;
            } catch (ForceDisconnectException e) {
                logForceDisconnectException(e);
                shutdownAsync();
            } catch (HttpHostConnectException e) {
                retry = true;
                Agent.LOG.log(Level.INFO,
                                     "An connection error occurred contacting {0}.  Please check your network / proxy"
                                             + " settings.", new Object[] {e.getHost()});

                Agent.LOG.log(Level.FINEST, e, e.toString(), new Object[0]);
            } catch (Exception e) {
                logMetricDataError(e);
                retry = true;
                String message = e.getMessage().toLowerCase();

                if ((message.contains("json")) && (message.contains("parse"))) {
                    retry = false;
                }
            }
            long duration = System.nanoTime() - startTime;
            if (retry) {
                retryCount.getAndIncrement();
            } else {
                retryCount.set(0);
                statsEngine.clear();
                recordSupportabilityMetrics(statsEngine, reportInterval, duration, data.size());
            }
        }
    }

    private void recordSupportabilityMetrics(StatsEngine statsEngine, long reportInterval, long duration,
                                             int dataSize) {
        if (reportInterval > 0L) {
            statsEngine.getResponseTimeStats("Supportability/MetricHarvest/interval")
                    .recordResponseTime(reportInterval, TimeUnit.MILLISECONDS);
        }

        statsEngine.getResponseTimeStats("Supportability/MetricHarvest/transmit")
                .recordResponseTime(duration, TimeUnit.NANOSECONDS);

        statsEngine.getStats("Supportability/MetricHarvest/count").incrementCallCount(dataSize);
    }

    private List<List<?>> sendMetricDataSyncRestart(long beginTimeMillis, long endTimeMillis,
                                                    List<MetricData> metricData) throws Exception {
        try {
            return dataSender.sendMetricData(beginTimeMillis, endTimeMillis, metricData);
        } catch (ForceRestartException e) {
            logForceRestartException(e);
            reconnectSync();
        }
        return dataSender.sendMetricData(beginTimeMillis, endTimeMillis, metricData);
    }

    private void registerMetricIds(List<List<?>> responseList) {
        for (List response : responseList) {
            JSONObject jsonObj = (JSONObject) JSONObject.class.cast(response.get(0));
            MetricName metricName = MetricName.parseJSON(jsonObj);
            Long id = (Long) Long.class.cast(response.get(1));
            metricIdRegistry.setMetricId(metricName, Integer.valueOf(id.intValue()));
        }
    }

    private void logMetricDataError(Exception e) {
        Agent.LOG.log(Level.INFO,
                             "An unexpected error occurred sending metric data to New Relic.  Please file a support "
                                     + "ticket once you have seen several of these messages in a short period of "
                                     + "time: {0}",
                             new Object[] {e.toString()});

        Agent.LOG.log(Level.FINEST, e, e.toString(), new Object[0]);
    }

    private void handle503Error(Exception e) {
        String msg =
                "A 503 (Unavailable) response was received while sending metric data to New Relic.  The agent will "
                        + "continue to aggregate data and report it in the next time period.";

        if (last503Error.getAndIncrement() == 5) {
            Agent.LOG.info(msg);
            Agent.LOG.log(Level.FINEST, e, e.toString(), new Object[0]);
        } else {
            Agent.LOG.log(Level.FINER, msg, e);
        }
    }

    protected void doStop() {
        try {
            shutdown();
        } catch (Exception e) {
            Level level = (e instanceof ConnectException) ? Level.FINER : Level.SEVERE;
            Agent.LOG.log(level, "An error occurred in the NewRelic agent shutdown", e);
        }
        ServiceFactory.getEnvironmentService().getEnvironment().removeEnvironmentChangeListener(this);
        ServiceFactory.getConfigService().removeIAgentConfigListener(this);
        ServiceFactory.getServiceManager().getCircuitBreakerService().removeRPMService(this);
    }

    public long getConnectionTimestamp() {
        return connectionTimestamp;
    }

    public void agentIdentityChanged(AgentIdentity agentIdentity) {
        if (connected) {
            logger.log(Level.FINE, "Reconnecting after an environment change");
            reconnect();
        }
    }

    public void configChanged(String appName, AgentConfig agentConfig) {
        last503Error.set(0);
    }
}