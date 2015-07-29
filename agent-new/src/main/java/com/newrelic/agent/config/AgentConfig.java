package com.newrelic.agent.config;

import java.util.List;
import java.util.Set;

public abstract interface AgentConfig extends com.newrelic.api.agent.Config
{
  public abstract boolean isAgentEnabled();

  public abstract String getLicenseKey();

  public abstract String getApplicationName();

  public abstract List<String> getApplicationNames();

  public abstract boolean isAutoAppNamingEnabled();

  public abstract boolean isAutoTransactionNamingEnabled();

  public abstract long getApdexTInMillis();

  public abstract long getApdexTInMillis(String paramString);

  public abstract boolean isApdexTSet();

  public abstract boolean isApdexTSet(String paramString);

  public abstract String getHost();

  public abstract int getTransactionSizeLimit();

  public abstract boolean isSyncStartup();

  public abstract boolean waitForRPMConnect();

  public abstract TransactionTracerConfig getTransactionTracerConfig();

  public abstract ClassTransformerConfig getClassTransformerConfig();

  public abstract BrowserMonitoringConfig getBrowserMonitoringConfig();

  public abstract TransactionTracerConfig getRequestTransactionTracerConfig();

  public abstract TransactionTracerConfig getBackgroundTransactionTracerConfig();

  public abstract ErrorCollectorConfig getErrorCollectorConfig();

  public abstract ThreadProfilerConfig getThreadProfilerConfig();

  public abstract SqlTraceConfig getSqlTraceConfig();

  public abstract JmxConfig getJmxConfig();

  public abstract JarCollectorConfig getJarCollectorConfig();

  public abstract ReinstrumentConfig getReinstrumentConfig();

  public abstract CrossProcessConfig getCrossProcessConfig();

  public abstract boolean isSessionCountTrackingEnabled();

  public abstract String getLanguage();

  public abstract <T> T getProperty(String paramString);

  public abstract <T> T getProperty(String paramString, T paramT);

  public abstract int getPort();

  public abstract boolean isSSL();

  public abstract boolean isAuditMode();

  public abstract String getProxyHost();

  public abstract Integer getProxyPort();

  public abstract String getProxyPassword();

  public abstract String getProxyUser();

  public abstract boolean isSendEnvironmentInfo();

  public abstract String getApiHost();

  public abstract int getApiPort();

  public abstract boolean isDebugEnabled();

  public abstract boolean isReportSqlParserErrors();

  public abstract boolean isLoggingToStdOut();

  public abstract String getLogFileName();

  public abstract String getLogFilePath();

  public abstract String getLogLevel();

  public abstract List<String> getIgnoreJars();

  public abstract int getLogLimit();

  public abstract int getLogFileCount();

  public abstract boolean isLogDaily();

  public abstract boolean isSendDataOnExit();

  public abstract long getSendDataOnExitThresholdInMillis();

  public abstract boolean isCpuSamplingEnabled();

  public abstract boolean isSendJvmProps();

  public abstract boolean isUsePrivateSSL();

  public abstract boolean isXraySessionEnabled();

  public abstract boolean isTrimStats();

  public abstract boolean isPlatformInformationEnabled();

  public abstract Set<String> getJDBCSupport();

  public abstract boolean isGenericJDBCSupportEnabled();

  public abstract int getMaxStackTraceLines();

  public abstract Config getInstrumentationConfig();

  public abstract int getMaxUserParameters();

  public abstract int getMaxUserParameterSize();

  public abstract boolean isHighSecurity();

  public abstract boolean getIbmWorkaroundEnabled();

  public abstract LabelsConfig getLabelsConfig();

  public abstract boolean isStartupTimingEnabled();

  public abstract CircuitBreakerConfig getCircuitBreakerConfig();

  public abstract StripExceptionConfig getStripExceptionConfig();
}