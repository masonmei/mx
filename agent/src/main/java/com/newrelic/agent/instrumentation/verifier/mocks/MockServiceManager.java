package com.newrelic.agent.instrumentation.verifier.mocks;

import java.util.Map;

import com.newrelic.agent.HarvestService;
import com.newrelic.agent.IAgent;
import com.newrelic.agent.RPMServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.TracerService;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.browser.BrowserService;
import com.newrelic.agent.cache.CacheService;
import com.newrelic.agent.circuitbreaker.CircuitBreakerService;
import com.newrelic.agent.commands.CommandParser;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.extension.ExtensionService;
import com.newrelic.agent.instrumentation.ClassTransformerService;
import com.newrelic.agent.jmx.JmxService;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.normalization.NormalizationService;
import com.newrelic.agent.profile.ProfilerService;
import com.newrelic.agent.reinstrument.RemoteInstrumentationService;
import com.newrelic.agent.rpm.RPMConnectionService;
import com.newrelic.agent.samplers.SamplerService;
import com.newrelic.agent.service.Service;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.service.analytics.InsightsService;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.service.async.AsyncTransactionService;
import com.newrelic.agent.service.module.JarCollectorService;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.utilization.UtilizationService;
import com.newrelic.agent.xray.IXRaySessionService;

public class MockServiceManager implements ServiceManager {
    private ConfigService configService = null;
    private StatsService statsService = null;
    private ExtensionService extensionService = null;
    private ClassTransformerService classTransformerService = null;
    private IAgent agent = null;
    private JarCollectorService jarCollectorService = null;
    private ThreadService threadService = null;

    public String getName() {
        return null;
    }

    public void start() throws Exception {
    }

    public void stop() throws Exception {
    }

    public boolean isEnabled() {
        return false;
    }

    public IAgentLogger getLogger() {
        return null;
    }

    public boolean isStarted() {
        return false;
    }

    public boolean isStopped() {
        return false;
    }

    public boolean isStartedOrStarting() {
        return false;
    }

    public boolean isStoppedOrStopping() {
        return false;
    }

    public Map<String, Map<String, Object>> getServicesConfiguration() {
        return null;
    }

    public void addService(Service service) {
    }

    public Service getService(String name) {
        return null;
    }

    public ExtensionService getExtensionService() {
        return extensionService;
    }

    public void setExtensionService(ExtensionService extensionService) {
        this.extensionService = extensionService;
    }

    public ProfilerService getProfilerService() {
        return null;
    }

    public TracerService getTracerService() {
        return null;
    }

    public TransactionTraceService getTransactionTraceService() {
        return null;
    }

    public ThreadService getThreadService() {
        return threadService;
    }

    public void setThreadService(ThreadService threadService) {
        this.threadService = threadService;
    }

    public HarvestService getHarvestService() {
        return null;
    }

    public SqlTraceService getSqlTraceService() {
        return null;
    }

    public CacheService getCacheService() {
        return null;
    }

    public DatabaseService getDatabaseService() {
        return null;
    }

    public TransactionService getTransactionService() {
        return null;
    }

    public JarCollectorService getJarCollectorService() {
        return jarCollectorService;
    }

    public void setJarCollectorService(JarCollectorService jarCollectorService) {
        this.jarCollectorService = jarCollectorService;
    }

    public JmxService getJmxService() {
        return null;
    }

    public TransactionEventsService getTransactionEventsService() {
        return null;
    }

    public CommandParser getCommandParser() {
        return null;
    }

    public RPMServiceManager getRPMServiceManager() {
        return null;
    }

    public SamplerService getSamplerService() {
        return null;
    }

    public IAgent getAgent() {
        return agent;
    }

    public void setAgent(IAgent agent) {
        this.agent = agent;
    }

    public ConfigService getConfigService() {
        return configService;
    }

    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }

    public RPMConnectionService getRPMConnectionService() {
        return null;
    }

    public EnvironmentService getEnvironmentService() {
        return null;
    }

    public ClassTransformerService getClassTransformerService() {
        return classTransformerService;
    }

    public void setClassTransformerService(ClassTransformerService classTransformerService) {
        this.classTransformerService = classTransformerService;
    }

    public StatsService getStatsService() {
        return statsService;
    }

    public void setStatsService(StatsService statsService) {
        this.statsService = statsService;
    }

    public NormalizationService getNormalizationService() {
        return null;
    }

    public IXRaySessionService getXRaySessionService() {
        return null;
    }

    public AttributesService getAttributesService() {
        return null;
    }

    public RemoteInstrumentationService getRemoteInstrumentationService() {
        return null;
    }

    public InsightsService getInsights() {
        return null;
    }

    public CircuitBreakerService getCircuitBreakerService() {
        return null;
    }

    public BrowserService getBrowserService() {
        return null;
    }

    public AsyncTransactionService getAsyncTxService() {
        return null;
    }

    public UtilizationService getUtilizationService() {
        return null;
    }
}