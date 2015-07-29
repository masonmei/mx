package com.newrelic.agent.service;

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
import com.newrelic.agent.normalization.NormalizationService;
import com.newrelic.agent.profile.ProfilerService;
import com.newrelic.agent.reinstrument.RemoteInstrumentationService;
import com.newrelic.agent.rpm.RPMConnectionService;
import com.newrelic.agent.samplers.SamplerService;
import com.newrelic.agent.service.analytics.InsightsService;
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.service.async.AsyncTransactionService;
import com.newrelic.agent.service.module.JarCollectorService;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.utilization.UtilizationService;
import com.newrelic.agent.xray.IXRaySessionService;

public abstract interface ServiceManager extends Service {
    public abstract Map<String, Map<String, Object>> getServicesConfiguration();

    public abstract void addService(Service paramService);

    public abstract Service getService(String paramString);

    public abstract ExtensionService getExtensionService();

    public abstract ProfilerService getProfilerService();

    public abstract TracerService getTracerService();

    public abstract TransactionTraceService getTransactionTraceService();

    public abstract ThreadService getThreadService();

    public abstract HarvestService getHarvestService();

    public abstract SqlTraceService getSqlTraceService();

    public abstract BrowserService getBrowserService();

    public abstract CacheService getCacheService();

    public abstract DatabaseService getDatabaseService();

    public abstract TransactionService getTransactionService();

    public abstract JarCollectorService getJarCollectorService();

    public abstract JmxService getJmxService();

    public abstract TransactionEventsService getTransactionEventsService();

    public abstract CommandParser getCommandParser();

    public abstract RPMServiceManager getRPMServiceManager();

    public abstract SamplerService getSamplerService();

    public abstract IAgent getAgent();

    public abstract ConfigService getConfigService();

    public abstract RPMConnectionService getRPMConnectionService();

    public abstract EnvironmentService getEnvironmentService();

    public abstract ClassTransformerService getClassTransformerService();

    public abstract StatsService getStatsService();

    public abstract NormalizationService getNormalizationService();

    public abstract RemoteInstrumentationService getRemoteInstrumentationService();

    public abstract IXRaySessionService getXRaySessionService();

    public abstract AttributesService getAttributesService();

    public abstract InsightsService getInsights();

    public abstract AsyncTransactionService getAsyncTxService();

    public abstract CircuitBreakerService getCircuitBreakerService();

    public abstract UtilizationService getUtilizationService();
}