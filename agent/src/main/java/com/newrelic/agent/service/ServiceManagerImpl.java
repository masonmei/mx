//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.newrelic.agent.GCService;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.HarvestServiceImpl;
import com.newrelic.agent.IAgent;
import com.newrelic.agent.RPMServiceManager;
import com.newrelic.agent.RPMServiceManagerImpl;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.TracerService;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.browser.BrowserService;
import com.newrelic.agent.browser.BrowserServiceImpl;
import com.newrelic.agent.cache.CacheService;
import com.newrelic.agent.circuitbreaker.CircuitBreakerService;
import com.newrelic.agent.commands.CommandParser;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.ConfigurationException;
import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.deadlock.DeadlockDetectorService;
import com.newrelic.agent.environment.EnvironmentService;
import com.newrelic.agent.environment.EnvironmentServiceImpl;
import com.newrelic.agent.extension.ExtensionService;
import com.newrelic.agent.instrumentation.ClassTransformerService;
import com.newrelic.agent.instrumentation.ClassTransformerServiceImpl;
import com.newrelic.agent.jmx.JmxService;
import com.newrelic.agent.normalization.NormalizationService;
import com.newrelic.agent.normalization.NormalizationServiceImpl;
import com.newrelic.agent.profile.ProfilerService;
import com.newrelic.agent.reinstrument.RemoteInstrumentationService;
import com.newrelic.agent.reinstrument.RemoteInstrumentationServiceImpl;
import com.newrelic.agent.rpm.RPMConnectionService;
import com.newrelic.agent.rpm.RPMConnectionServiceImpl;
import com.newrelic.agent.samplers.CPUSamplerService;
import com.newrelic.agent.samplers.NoopSamplerService;
import com.newrelic.agent.samplers.SamplerService;
import com.newrelic.agent.samplers.SamplerServiceImpl;
import com.newrelic.agent.service.analytics.InsightsService;
import com.newrelic.agent.service.analytics.InsightsServiceImpl;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.service.async.AsyncTransactionService;
import com.newrelic.agent.service.module.JarCollectorService;
import com.newrelic.agent.service.module.JarCollectorServiceImpl;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.sql.SqlTraceServiceImpl;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.stats.StatsWork;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.utilization.UtilizationService;
import com.newrelic.agent.xray.IXRaySessionService;
import com.newrelic.agent.xray.XRaySessionService;
import com.newrelic.api.agent.MetricAggregator;
import com.sun.org.apache.xpath.internal.operations.Bool;
import com.sun.tools.corba.se.idl.constExpr.BooleanNot;

public class ServiceManagerImpl extends AbstractService implements ServiceManager {
    private final Map<String, Service> services = new HashMap();
    private final IAgent agentService;
    private final List<StatsWork> statsWork = Lists.newArrayList();
    private volatile ExtensionService extensionService;
    private volatile ProfilerService profilerService;
    private volatile TracerService tracerService;
    private volatile TransactionTraceService transactionTraceService;
    private volatile ThreadService threadService;
    private volatile HarvestService harvestService;
    private volatile Service gcService;
    private volatile TransactionService transactionService;
    private volatile JmxService jmxService;
    private volatile TransactionEventsService transactionEventsService;
    private volatile CommandParser commandParser;
    private volatile RPMServiceManager rpmServiceManager;
    private volatile Service cpuSamplerService;
    private volatile DeadlockDetectorService deadlockDetectorService;
    private volatile SamplerService samplerService;
    private volatile ConfigService configService;
    private volatile RPMConnectionService rpmConnectionService;
    private volatile EnvironmentService environmentService;
    private volatile ClassTransformerService classTransformerService;
    private volatile StatsService statsService = new ServiceManagerImpl.InitialStatsService();
    private volatile SqlTraceService sqlTraceService;
    private volatile DatabaseService databaseService;
    private volatile BrowserService browserService;
    private volatile JarCollectorService jarCollectorService;
    private volatile CacheService cacheService;
    private volatile NormalizationService normalizationService;
    private volatile RemoteInstrumentationService remoteInstrumentationService;
    private volatile IXRaySessionService xrayService;
    private volatile AttributesService attsService;
    private volatile UtilizationService utilizationService;
    private volatile InsightsService insightsService;
    private volatile AsyncTransactionService asyncTxService;
    private volatile CircuitBreakerService circuitBreakerService;

    public ServiceManagerImpl(IAgent agent) throws ConfigurationException {
        super(ServiceManagerImpl.class.getSimpleName());
        this.agentService = agent;
        this.configService = ConfigServiceFactory.createConfigService();
    }

    public boolean isEnabled() {
        return true;
    }

    protected synchronized void doStart() throws Exception {
        this.agentService.start();
        this.threadService = new ThreadService();
        this.circuitBreakerService = new CircuitBreakerService();
        this.classTransformerService = new ClassTransformerServiceImpl(this.agentService.getInstrumentation());
        this.jmxService = new JmxService();
        this.extensionService = new ExtensionService();
        this.jarCollectorService = new JarCollectorServiceImpl();
        this.tracerService = new TracerService();
        this.asyncTxService = new AsyncTransactionService();
        this.environmentService = new EnvironmentServiceImpl();
        this.cacheService = new CacheService();
        this.extensionService.start();
        this.classTransformerService.start();
        boolean realAgent = this.agentService.getInstrumentation() != null;
        this.statsService = new StatsServiceImpl();
        this.replayStartupStatsWork();
        this.utilizationService = new UtilizationService();
        this.utilizationService.start();
        this.rpmConnectionService = new RPMConnectionServiceImpl();
        this.transactionService = new TransactionService();
        this.rpmServiceManager = new RPMServiceManagerImpl();
        this.normalizationService = new NormalizationServiceImpl();
        this.harvestService = new HarvestServiceImpl();
        this.gcService = (realAgent ? new GCService() : new NoopService("GC Service"));
        this.transactionTraceService = new TransactionTraceService();
        this.transactionEventsService = new TransactionEventsService();
        this.profilerService = new ProfilerService();
        this.commandParser = new CommandParser();
        this.cpuSamplerService = (realAgent ? new CPUSamplerService() : new NoopService("CPU Sampler"));
        this.deadlockDetectorService = new DeadlockDetectorService();
        this.samplerService = (realAgent ? new SamplerServiceImpl() : new NoopSamplerService());
        this.sqlTraceService = new SqlTraceServiceImpl();
        this.databaseService = new DatabaseService();
        this.browserService = new BrowserServiceImpl();
        this.remoteInstrumentationService = new RemoteInstrumentationServiceImpl();
        this.xrayService = new XRaySessionService();
        this.attsService = new AttributesService();
        this.insightsService = new InsightsServiceImpl();
        this.harvestService.addHarvestListener(this.extensionService);
        this.asyncTxService.start();
        this.threadService.start();
        this.statsService.start();
        this.environmentService.start();
        this.rpmConnectionService.start();
        this.tracerService.start();
        this.jarCollectorService.start();
        this.harvestService.start();
        this.gcService.start();
        this.transactionService.start();
        this.transactionTraceService.start();
        this.transactionEventsService.start();
        this.profilerService.start();
        this.commandParser.start();
        this.jmxService.start();
        this.cpuSamplerService.start();
        this.deadlockDetectorService.start();
        this.samplerService.start();
        this.sqlTraceService.start();
        this.browserService.start();
        this.cacheService.start();
        this.normalizationService.start();
        this.databaseService.start();
        this.configService.start();
        this.remoteInstrumentationService.start();
        this.xrayService.start();
        this.attsService.start();
        this.insightsService.start();
        this.circuitBreakerService.start();
        this.startServices();
        this.rpmServiceManager.start();
        ServiceTiming.setEndTime();
        ServiceTiming.logServiceTimings(this.getLogger());
    }

    protected synchronized void doStop() throws Exception {
        this.insightsService.stop();
        this.circuitBreakerService.stop();
        this.remoteInstrumentationService.stop();
        this.configService.stop();
        this.classTransformerService.stop();
        this.agentService.stop();
        this.rpmConnectionService.stop();
        this.deadlockDetectorService.stop();
        this.jarCollectorService.stop();
        this.harvestService.stop();
        this.cpuSamplerService.stop();
        this.samplerService.stop();
        this.sqlTraceService.stop();
        this.normalizationService.stop();
        this.databaseService.stop();
        this.extensionService.stop();
        this.transactionService.stop();
        this.tracerService.stop();
        this.threadService.stop();
        this.transactionTraceService.stop();
        this.transactionEventsService.stop();
        this.profilerService.stop();
        this.commandParser.stop();
        this.jmxService.stop();
        this.rpmServiceManager.stop();
        this.environmentService.stop();
        this.statsService.stop();
        this.browserService.stop();
        this.cacheService.stop();
        this.xrayService.stop();
        this.attsService.stop();
        this.stopServices();
    }

    private void startServices() throws Exception {
        for (Service service : this.getServices().values()) {
            service.start();
        }
    }

    private void stopServices() throws Exception {
        for (Service service : this.getServices().values()) {
            service.stop();
        }
    }

    public synchronized void addService(Service service) {
        this.services.put(service.getName(), service);
    }

    private Map<String, Service> getServices() {
        return Collections.unmodifiableMap(this.services);
    }

    public synchronized Map<String, Map<String, Object>> getServicesConfiguration() {
        HashMap config = new HashMap();
        HashMap serviceInfo = new HashMap();
        serviceInfo.put("enabled", Boolean.valueOf(this.transactionService.isEnabled()));
        config.put("TransactionService", serviceInfo);
        serviceInfo = new HashMap();
        config.put("TransactionTraceService", serviceInfo);
        serviceInfo.put("enabled", Boolean.valueOf(this.transactionTraceService.isEnabled()));
        serviceInfo = new HashMap();
        config.put("TransactionEventsService", serviceInfo);
        serviceInfo.put("enabled", Boolean.valueOf(this.transactionEventsService.isEnabled()));
        serviceInfo = new HashMap();
        serviceInfo.put("enabled", Boolean.valueOf(this.profilerService.isEnabled()));
        config.put("ProfilerService", serviceInfo);
        serviceInfo = new HashMap();
        serviceInfo.put("enabled", Boolean.valueOf(this.tracerService.isEnabled()));
        config.put("TracerService", serviceInfo);
        serviceInfo = new HashMap();
        serviceInfo.put("enabled", Boolean.valueOf(this.commandParser.isEnabled()));
        config.put("CommandParser", serviceInfo);
        serviceInfo = new HashMap();
        serviceInfo.put("enabled", Boolean.valueOf(this.jmxService.isEnabled()));
        config.put("JmxService", serviceInfo);
        serviceInfo = new HashMap();
        serviceInfo.put("enabled", Boolean.valueOf(this.threadService.isEnabled()));
        config.put("ThreadService", serviceInfo);
        serviceInfo = new HashMap();
        serviceInfo.put("enabled", Boolean.valueOf(this.deadlockDetectorService.isEnabled()));
        config.put("DeadlockService", serviceInfo);

        for (Service service : this.getServices().values()) {
            serviceInfo = new HashMap();
            serviceInfo.put("enabled", Boolean.valueOf(service.isEnabled()));
            config.put(service.getClass().getSimpleName(), serviceInfo);
        }

        return config;
    }

    public synchronized Service getService(String name) {
        return this.services.get(name);
    }

    public ExtensionService getExtensionService() {
        return this.extensionService;
    }

    public ProfilerService getProfilerService() {
        return this.profilerService;
    }

    public TracerService getTracerService() {
        return this.tracerService;
    }

    public TransactionTraceService getTransactionTraceService() {
        return this.transactionTraceService;
    }

    public ThreadService getThreadService() {
        return this.threadService;
    }

    public HarvestService getHarvestService() {
        return this.harvestService;
    }

    public TransactionService getTransactionService() {
        return this.transactionService;
    }

    public JmxService getJmxService() {
        return this.jmxService;
    }

    public TransactionEventsService getTransactionEventsService() {
        return this.transactionEventsService;
    }

    public CommandParser getCommandParser() {
        return this.commandParser;
    }

    public RPMServiceManager getRPMServiceManager() {
        return this.rpmServiceManager;
    }

    public SamplerService getSamplerService() {
        return this.samplerService;
    }

    public IAgent getAgent() {
        return this.agentService;
    }

    public ConfigService getConfigService() {
        return this.configService;
    }

    public RPMConnectionService getRPMConnectionService() {
        return this.rpmConnectionService;
    }

    public EnvironmentService getEnvironmentService() {
        return this.environmentService;
    }

    public ClassTransformerService getClassTransformerService() {
        return this.classTransformerService;
    }

    public StatsService getStatsService() {
        return this.statsService;
    }

    public SqlTraceService getSqlTraceService() {
        return this.sqlTraceService;
    }

    public DatabaseService getDatabaseService() {
        return this.databaseService;
    }

    public CacheService getCacheService() {
        return this.cacheService;
    }

    public AsyncTransactionService getAsyncTxService() {
        return this.asyncTxService;
    }

    public BrowserService getBrowserService() {
        return this.browserService;
    }

    public NormalizationService getNormalizationService() {
        return this.normalizationService;
    }

    public JarCollectorService getJarCollectorService() {
        return this.jarCollectorService;
    }

    public RemoteInstrumentationService getRemoteInstrumentationService() {
        return this.remoteInstrumentationService;
    }

    public IXRaySessionService getXRaySessionService() {
        return this.xrayService;
    }

    public AttributesService getAttributesService() {
        return this.attsService;
    }

    public InsightsService getInsights() {
        return this.insightsService;
    }

    public CircuitBreakerService getCircuitBreakerService() {
        return this.circuitBreakerService;
    }

    private void replayStartupStatsWork() {
        for (StatsWork work : this.statsWork) {
            this.statsService.doStatsWork(work);
        }

        this.statsWork.clear();
    }

    public UtilizationService getUtilizationService() {
        return this.utilizationService;
    }

    private class InitialStatsService extends AbstractService implements StatsService {
        private final MetricAggregator metricAggregator = new StatsServiceMetricAggregator(this);

        protected InitialStatsService() {
            super("Bootstrap stats service");
        }

        public boolean isEnabled() {
            return true;
        }

        public void doStatsWork(StatsWork statsWork) {
            ServiceManagerImpl.this.statsWork.add(statsWork);
        }

        public StatsEngine getStatsEngineForHarvest(String appName) {
            return null;
        }

        protected void doStart() throws Exception {
        }

        protected void doStop() throws Exception {
        }

        public MetricAggregator getMetricAggregator() {
            return this.metricAggregator;
        }
    }
}
