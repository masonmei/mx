package com.newrelic.agent.service;

import com.newrelic.agent.HarvestService;
import com.newrelic.agent.IAgent;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.RPMServiceManager;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.TracerService;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.browser.BrowserService;
import com.newrelic.agent.cache.CacheService;
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
import com.newrelic.agent.service.analytics.TransactionEventsService;
import com.newrelic.agent.service.async.AsyncTransactionService;
import com.newrelic.agent.service.module.JarCollectorService;
import com.newrelic.agent.sql.SqlTraceService;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.utilization.UtilizationService;
import com.newrelic.agent.xray.IXRaySessionService;
import java.util.Map;

public class ServiceFactory
{
  private static volatile ServiceManager SERVICE_MANAGER;

  public static void setServiceManager(ServiceManager serviceManager)
  {
    if (serviceManager != null)
      SERVICE_MANAGER = serviceManager;
  }

  public static ServiceManager getServiceManager()
  {
    return SERVICE_MANAGER;
  }

  public static StatsService getStatsService() {
    return SERVICE_MANAGER.getStatsService();
  }

  public static ClassTransformerService getClassTransformerService() {
    return SERVICE_MANAGER.getClassTransformerService();
  }

  public static EnvironmentService getEnvironmentService() {
    return SERVICE_MANAGER.getEnvironmentService();
  }

  public static ConfigService getConfigService() {
    return SERVICE_MANAGER.getConfigService();
  }

  public static RPMConnectionService getRPMConnectionService() {
    return SERVICE_MANAGER.getRPMConnectionService();
  }

  public static IAgent getAgent() {
    return SERVICE_MANAGER.getAgent();
  }

  public static SamplerService getSamplerService() {
    return SERVICE_MANAGER.getSamplerService();
  }

  public static RPMServiceManager getRPMServiceManager() {
    return SERVICE_MANAGER.getRPMServiceManager();
  }

  public static IRPMService getRPMService() {
    return SERVICE_MANAGER.getRPMServiceManager().getRPMService();
  }

  public static IRPMService getRPMService(String appName) {
    return SERVICE_MANAGER.getRPMServiceManager().getRPMService(appName);
  }

  public static CommandParser getCommandParser() {
    return SERVICE_MANAGER.getCommandParser();
  }

  public static JmxService getJmxService() {
    return SERVICE_MANAGER.getJmxService();
  }

  public static TransactionEventsService getTransactionEventsService() {
    return SERVICE_MANAGER.getTransactionEventsService();
  }

  public static SqlTraceService getSqlTraceService() {
    return SERVICE_MANAGER.getSqlTraceService();
  }

  public static BrowserService getBeaconService() {
    return SERVICE_MANAGER.getBrowserService();
  }

  public static CacheService getCacheService() {
    return SERVICE_MANAGER.getCacheService();
  }

  public static NormalizationService getNormalizationService() {
    return SERVICE_MANAGER.getNormalizationService();
  }

  public static DatabaseService getDatabaseService() {
    return SERVICE_MANAGER.getDatabaseService();
  }

  public static TransactionService getTransactionService() {
    return SERVICE_MANAGER.getTransactionService();
  }

  public static HarvestService getHarvestService() {
    return SERVICE_MANAGER.getHarvestService();
  }

  public static RemoteInstrumentationService getRemoteInstrumentationService() {
    return SERVICE_MANAGER.getRemoteInstrumentationService();
  }

  public static ThreadService getThreadService() {
    return SERVICE_MANAGER.getThreadService();
  }

  public static TransactionTraceService getTransactionTraceService() {
    return SERVICE_MANAGER.getTransactionTraceService();
  }

  public static TracerService getTracerService() {
    return SERVICE_MANAGER.getTracerService();
  }

  public static ProfilerService getProfilerService() {
    return SERVICE_MANAGER.getProfilerService();
  }

  public static ExtensionService getExtensionService() {
    return SERVICE_MANAGER.getExtensionService();
  }

  public static JarCollectorService getJarCollectorService() {
    return SERVICE_MANAGER.getJarCollectorService();
  }

  public static IXRaySessionService getXRaySessionService() {
    return SERVICE_MANAGER.getXRaySessionService();
  }

  public static AttributesService getAttributesService() {
    return SERVICE_MANAGER.getAttributesService();
  }

  public static Service getService(String name) {
    return SERVICE_MANAGER.getService(name);
  }

  public static void addService(Service service) {
    SERVICE_MANAGER.addService(service);
  }

  public static Map<String, Map<String, Object>> getServicesConfiguration() {
    return SERVICE_MANAGER.getServicesConfiguration();
  }

  public static AsyncTransactionService getAsyncTxService() {
    return SERVICE_MANAGER.getAsyncTxService();
  }

  public static UtilizationService getUtilizationService() {
    return SERVICE_MANAGER.getUtilizationService();
  }
}