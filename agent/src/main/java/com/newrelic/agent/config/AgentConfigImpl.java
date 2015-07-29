package com.newrelic.agent.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.deps.com.google.common.collect.Maps;

public class AgentConfigImpl extends BaseConfig implements AgentConfig {
    public static final String APDEX_T = "apdex_t";
    public static final String API_HOST = "api_host";
    public static final String API_PORT = "api_port";
    public static final String APP_NAME = "app_name";
    public static final String AUDIT_MODE = "audit_mode";
    public static final String BROWSER_MONITORING = "browser_monitoring";
    public static final String ATTRIBUTES = "attributes";
    public static final String CAPTURE_PARAMS = "capture_params";
    public static final String CAPTURE_MESSAGING_PARAMS = "capture_messaging_params";
    public static final String CLASS_TRANSFORMER = "class_transformer";
    public static final String CPU_SAMPLING_ENABLED = "cpu_sampling_enabled";
    public static final String CROSS_APPLICATION_TRACER = "cross_application_tracer";
    public static final String DEBUG = "newrelic.debug";
    public static final String AGENT_ENABLED = "agent_enabled";
    public static final String ENABLED = "enabled";
    public static final String ENABLE_AUTO_APP_NAMING = "enable_auto_app_naming";
    public static final String ENABLE_AUTO_TRANSACTION_NAMING = "enable_auto_transaction_naming";
    public static final String ENABLE_BOOTSTRAP_CLASS_INSTRUMENTATION = "enable_bootstrap_class_instrumentation";
    public static final String ENABLE_CLASS_RETRANSFORMATION = "enable_class_retransformation";
    public static final String ENABLE_CUSTOM_TRACING = "enable_custom_tracing";
    public static final String ENABLE_SESSION_COUNT_TRACKING = "enable_session_count_tracking";
    public static final String ERROR_COLLECTOR = "error_collector";
    public static final String HIGH_SECURITY = "high_security";
    public static final String JMX = "jmx";
    public static final String JAR_COLLECTOR = "jar_collector";
    public static final String ANALYTICS_EVENTS = "analytics_events";
    public static final String TRANSACTION_EVENTS = "transaction_events";
    public static final String CUSTOM_INSIGHT_EVENTS = "custom_insights_events";
    public static final String USE_PRIVATE_SSL = "use_private_ssl";
    public static final String REINSTRUMENT = "reinstrument";
    public static final String XRAY_SESSIONS_ENABLED = "xray_session_enabled";
    public static final String PLATFORM_INFORMATION_ENABLED = "platform_information_enabled";
    public static final String IBM = "ibm";
    public static final String EXT_CONFIG_DIR = "extensions.dir";
    public static final String HOST = "host";
    public static final String IGNORE_JARS = "ignore_jars";
    public static final String IS_SSL = "ssl";
    public static final String LABELS = "labels";
    public static final String LANGUAGE = "language";
    public static final String LICENSE_KEY = "license_key";
    public static final String LOG_FILE_COUNT = "log_file_count";
    public static final String LOG_FILE_NAME = "log_file_name";
    public static final String LOG_FILE_PATH = "log_file_path";
    public static final String LOG_LEVEL = "log_level";
    public static final String LOG_LIMIT = "log_limit_in_kbytes";
    public static final String LOG_DAILY = "log_daily";
    public static final int MAX_USER_PARAMETERS = 64;
    public static final int MAX_USER_PARAMETER_SIZE = 255;
    public static final String KEY_TRANSACTIONS = "web_transactions_apdex";
    public static final String PORT = "port";
    public static final String PROXY_HOST = "proxy_host";
    public static final String PROXY_PORT = "proxy_port";
    public static final String PROXY_USER = "proxy_user";
    public static final String PROXY_PASS = "proxy_password";
    public static final String REPORT_SQL_PARSER_ERRORS = "report_sql_parser_errors";
    public static final String SEND_DATA_ON_EXIT = "send_data_on_exit";
    public static final String SEND_DATA_ON_EXIT_THRESHOLD = "send_data_on_exit_threshold";
    public static final String SEND_ENVIRONMENT_INFO = "send_environment_info";
    public static final String SEND_JVM_PROPERY = "send_jvm_props";
    public static final String SLOW_SQL = "slow_sql";
    public static final String STARTUP_LOG_LEVEL = "startup_log_level";
    public static final String STDOUT = "STDOUT";
    public static final String SYNC_STARTUP = "sync_startup";
    public static final String STARTUP_TIMING = "startup_timing";
    public static final String STRIP_EXCEPTION_MESSAGES = "strip_exception_messages";
    public static final String THREAD_PROFILER = "thread_profiler";
    public static final String TRANSACTION_SIZE_LIMIT = "transaction_size_limit";
    public static final String TRANSACTION_TRACER = "transaction_tracer";
    public static final String THREAD_CPU_TIME_ENABLED = "thread_cpu_time_enabled";
    public static final String THREAD_PROFILER_ENABLED = "enabled";
    public static final String TRACE_DATA_CALLS = "trace_data_calls";
    public static final String TRIM_STATS = "trim_stats";
    public static final String WAIT_FOR_RPM_CONNECT = "wait_for_rpm_connect";
    public static final double DEFAULT_APDEX_T = 1.0D;
    public static final String DEFAULT_API_HOST = "rpm.newrelic.com";
    public static final boolean DEFAULT_AUDIT_MODE = false;
    public static final boolean DEFAULT_CPU_SAMPLING_ENABLED = true;
    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_ENABLE_AUTO_APP_NAMING = false;
    public static final boolean DEFAULT_ENABLE_AUTO_TRANSACTION_NAMING = true;
    public static final boolean DEFAULT_ENABLE_CUSTOM_TRACING = true;
    public static final boolean DEFAULT_ENABLE_SESSION_COUNT_TRACKING = false;
    public static final boolean DEFAULT_HIGH_SECURITY = false;
    public static final boolean DEFAULT_PLATFORM_INFORMATION_ENABLED = true;
    public static final String DEFAULT_HOST = "collector.newrelic.com";
    public static final boolean DEFAULT_IS_SSL = true;
    public static final String DEFAULT_LANGUAGE = "java";
    public static final int DEFAULT_LOG_FILE_COUNT = 1;
    public static final String DEFAULT_LOG_FILE_NAME = "newrelic_agent.log";
    public static final String DEFAULT_LOG_LEVEL = "info";
    public static final int DEFAULT_LOG_LIMIT = 0;
    public static final boolean DEFAULT_LOG_DAILY = false;
    public static final int DEFAULT_PORT = 80;
    public static final String DEFAULT_PROXY_HOST = null;
    public static final int DEFAULT_PROXY_PORT = 8080;
    public static final boolean DEFAULT_REPORT_SQL_PARSER_ERRORS = false;
    public static final boolean DEFAULT_SEND_DATA_ON_EXIT = false;
    public static final int DEFAULT_SEND_DATA_ON_EXIT_THRESHOLD = 60;
    public static final boolean DEFAULT_SEND_ENVIRONMENT_INFO = true;
    public static final int DEFAULT_SSL_PORT = 443;
    public static final boolean DEFAULT_SYNC_STARTUP = false;
    public static final boolean DEFAULT_STARTUP_TIMING = true;
    public static final boolean DEFAULT_TRACE_DATA_CALLS = false;
    public static final int DEFAULT_TRANSACTION_SIZE_LIMIT = 2000;
    public static final boolean DEFAULT_TRIM_STATS = true;
    public static final boolean DEFAULT_WAIT_FOR_RPM_CONNECT = true;
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.";
    public static final boolean DEFAULT_USE_PRIVATE_SSL = false;
    public static final boolean DEFAULT_XRAY_SESSIONS_ENABLED = true;
    public static final String IBM_WORKAROUND = "ibm_iv25688_workaround";
    public static final boolean IBM_WORKAROUND_DEFAULT = IBMUtils.getIbmWorkaroundDefault();
    public static final String GENERIC_JDBC_SUPPORT = "generic";
    public static final String MYSQL_JDBC_SUPPORT = "mysql";
    private final boolean highSecurity;
    private final long apdexTInMillis;
    private final boolean enabled;
    private final boolean debug;
    private final String licenseKey;
    private final String host;
    private final int port;
    private final Integer proxyPort;
    private final boolean isSSL;
    private final List<String> ignoreJars;
    private final String appName;
    private final List<String> appNames;
    private final boolean cpuSamplingEnabled;
    private final boolean autoAppNamingEnabled;
    private final boolean autoTransactionNamingEnabled;
    private final String logLevel;
    private final boolean logDaily;
    private final String proxyHost;
    private final String proxyUser;
    private final String proxyPass;
    private final boolean sessionCountTrackingEnabled;
    private final int transactionSizeLimit;
    private final boolean reportSqlParserErrors;
    private final boolean auditMode;
    private final boolean waitForRPMConnect;
    private final boolean startupTimingEnabled;
    private final TransactionTracerConfigImpl transactionTracerConfig;
    private final ErrorCollectorConfig errorCollectorConfig;
    private final SqlTraceConfig sqlTraceConfig;
    private final ThreadProfilerConfig threadProfilerConfig;
    private final TransactionTracerConfigImpl requestTransactionTracerConfig;
    private final TransactionTracerConfigImpl backgroundTransactionTracerConfig;
    private final BrowserMonitoringConfig browserMonitoringConfig;
    private final ClassTransformerConfig classTransformerConfig;
    private final KeyTransactionConfig keyTransactionConfig;
    private final JmxConfig jmxConfig;
    private final JarCollectorConfig jarCollectorConfig;
    private final ReinstrumentConfig reinstrumentConfig;
    private final CrossProcessConfig crossProcessConfig;
    private final LabelsConfig labelsConfig;
    private final CircuitBreakerConfig circuitBreakerConfig;
    private final StripExceptionConfig stripExceptionConfig;
    private final boolean isApdexTSet;
    private final boolean sendJvmProps;
    private final boolean usePrivateSSL;
    private final boolean xRaySessionsEnabled;
    private final boolean trimStats;
    private final boolean platformInformationEnabled;
    private final Map<String, Object> flattenedProperties;
    private final HashSet<String> jdbcSupport;
    private final boolean genericJdbcSupportEnabled;
    private final int maxStackTraceLines;
    private final boolean ibmWorkaroundEnabled;
    private final Config instrumentationConfig;

    private AgentConfigImpl(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);

        highSecurity = getProperty(HIGH_SECURITY, DEFAULT_HIGH_SECURITY);
        isSSL = initSsl(highSecurity, props);
        isApdexTSet = (getProperty(APDEX_T) != null);
        apdexTInMillis = ((long) (getDoubleProperty(APDEX_T, DEFAULT_APDEX_T) * 1000.0D));
        debug = true;
        enabled = ((getProperty(THREAD_PROFILER_ENABLED, DEFAULT_ENABLED)) && (getProperty(AGENT_ENABLED,
                                                                                                  DEFAULT_ENABLED)));
        licenseKey = (getProperty(LICENSE_KEY));
        host = (getProperty(HOST, DEFAULT_HOST));
        ignoreJars = new ArrayList<String>(getUniqueStrings(IGNORE_JARS, ","));
        logLevel = initLogLevel();
        logDaily = getProperty(LOG_DAILY, DEFAULT_LOG_DAILY);
        port = getIntProperty(PORT, isSSL ? DEFAULT_SSL_PORT : DEFAULT_PORT);
        proxyHost = (getProperty(PROXY_HOST, DEFAULT_PROXY_HOST));
        proxyPort = getIntProperty(PROXY_PORT, DEFAULT_PROXY_PORT);
        proxyUser = (getProperty(PROXY_USER));
        proxyPass = (getProperty(PROXY_PASS));
        appNames = new ArrayList<String>(getUniqueStrings(APP_NAME, ";"));
        appName = getFirstString(APP_NAME, ";");
        cpuSamplingEnabled = getProperty(CPU_SAMPLING_ENABLED, DEFAULT_CPU_SAMPLING_ENABLED);
        autoAppNamingEnabled = getProperty(ENABLE_AUTO_APP_NAMING, DEFAULT_ENABLE_AUTO_APP_NAMING);
        autoTransactionNamingEnabled =
                getProperty(ENABLE_AUTO_TRANSACTION_NAMING, DEFAULT_ENABLE_AUTO_TRANSACTION_NAMING);

        transactionSizeLimit = (getIntProperty(TRANSACTION_SIZE_LIMIT, DEFAULT_TRANSACTION_SIZE_LIMIT) * 1024);
        sessionCountTrackingEnabled = getProperty(ENABLE_SESSION_COUNT_TRACKING, DEFAULT_ENABLE_SESSION_COUNT_TRACKING);
        reportSqlParserErrors = getProperty(REPORT_SQL_PARSER_ERRORS, DEFAULT_REPORT_SQL_PARSER_ERRORS);
        auditMode = ((getProperty(TRACE_DATA_CALLS, DEFAULT_TRACE_DATA_CALLS)) || (getProperty(AUDIT_MODE,
                                                                                                      DEFAULT_AUDIT_MODE)));

        waitForRPMConnect = getProperty(WAIT_FOR_RPM_CONNECT, DEFAULT_WAIT_FOR_RPM_CONNECT);
        startupTimingEnabled = getProperty(STARTUP_TIMING, DEFAULT_STARTUP_TIMING);
        transactionTracerConfig = initTransactionTracerConfig(apdexTInMillis, highSecurity);
        requestTransactionTracerConfig =
                transactionTracerConfig.createRequestTransactionTracerConfig(apdexTInMillis, highSecurity);

        backgroundTransactionTracerConfig =
                transactionTracerConfig.createBackgroundTransactionTracerConfig(apdexTInMillis, highSecurity);

        errorCollectorConfig = initErrorCollectorConfig();
        threadProfilerConfig = initThreadProfilerConfig();
        keyTransactionConfig = initKeyTransactionConfig(apdexTInMillis);
        sqlTraceConfig = initSqlTraceConfig();
        browserMonitoringConfig = initBrowserMonitoringConfig();
        classTransformerConfig = initClassTransformerConfig();
        crossProcessConfig = initCrossProcessConfig();
        stripExceptionConfig = initStripExceptionConfig(highSecurity);
        labelsConfig = new LabelsConfigImpl(getProperty(LABELS));
        jmxConfig = initJmxConfig();
        jarCollectorConfig = initJarCollectorConfig();
        reinstrumentConfig = initReinstrumentConfig();
        circuitBreakerConfig = initCircuitBreakerConfig();
        sendJvmProps = getProperty(SEND_JVM_PROPERY, DEFAULT_ENABLED);
        usePrivateSSL = getProperty(USE_PRIVATE_SSL, DEFAULT_USE_PRIVATE_SSL);
        xRaySessionsEnabled = getProperty(XRAY_SESSIONS_ENABLED, DEFAULT_XRAY_SESSIONS_ENABLED);
        trimStats = getProperty(TRIM_STATS, DEFAULT_TRIM_STATS);
        platformInformationEnabled = getProperty(PLATFORM_INFORMATION_ENABLED, DEFAULT_PLATFORM_INFORMATION_ENABLED);
        ibmWorkaroundEnabled = getProperty(IBM_WORKAROUND, IBM_WORKAROUND_DEFAULT);

        instrumentationConfig = new BaseConfig(nestedProps("instrumentation"), "newrelic.config.instrumentation");

        maxStackTraceLines = getProperty("max_stack_trace_lines", 30);

        String[] jdbcSupport = (getProperty("jdbc_support", GENERIC_JDBC_SUPPORT)).split(",");
        this.jdbcSupport = new HashSet(Arrays.asList(jdbcSupport));
        genericJdbcSupportEnabled = this.jdbcSupport.contains(GENERIC_JDBC_SUPPORT);

        Map propsWithSystemProps = Maps.newHashMap(props);
        propsWithSystemProps
                .putAll(SystemPropertyFactory.getSystemPropertyProvider().getNewRelicPropertiesWithoutPrefix());

        Map flattenedProps = Maps.newHashMap();
        flatten("", propsWithSystemProps, flattenedProps);
        checkHighSecurityPropsInFlattened(flattenedProps);
        flattenedProperties = Collections.unmodifiableMap(flattenedProps);
    }

    public static AgentConfig createAgentConfig(Map<String, Object> settings) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new AgentConfigImpl(settings);
    }

    private void checkHighSecurityPropsInFlattened(Map<String, Object> flattenedProps) {
        if ((highSecurity) && (!flattenedProps.isEmpty())) {
            flattenedProps.put(IS_SSL, Boolean.valueOf(isSSL));
            flattenedProps.put("transaction_tracer.record_sql", transactionTracerConfig.getRecordSql());
        }
    }

    private boolean initSsl(boolean isHighSec, Map<String, Object> props) {
        boolean ssl;
        if (isHighSec) {
            ssl = true;
            props.put(IS_SSL, Boolean.TRUE);
        } else {
            ssl = getProperty(IS_SSL, DEFAULT_IS_SSL);
        }
        return ssl;
    }

    private void flatten(String prefix, Map<String, Object> source, Map<String, Object> dest) {
        for (Entry e : source.entrySet()) {
            if ((e.getValue() instanceof Map)) {
                flatten(prefix + e.getKey() + '.', (Map) e.getValue(), dest);
            } else {
                dest.put(prefix + e.getKey(), e.getValue());
            }
        }
    }

    public <T> T getValue(String path) {
        return getValue(path, null);
    }

    public <T> T getValue(String path, T defaultValue) {
        Object value = flattenedProperties.get(path);
        if (value == null) {
            return defaultValue;
        }
        if ((value instanceof ServerProp)) {
            value = ((ServerProp) value).getValue();
            return castValue(path, value, defaultValue);
        }
        if (((value instanceof String)) && ((defaultValue instanceof Boolean))) {
            value = Boolean.valueOf((String) value);
            return (T) value;
        }
        if (((value instanceof String)) && ((defaultValue instanceof Integer))) {
            value = Integer.valueOf((String) value);
            return (T) value;
        }
        try {
            return (T) value;
        } catch (ClassCastException ccx) {
            Agent.LOG.log(Level.FINE, "Using default value \"{0}\" for \"{1}\"", defaultValue, path);
        }
        return defaultValue;
    }

    private String initLogLevel() {
        Object val = getProperty(LOG_LEVEL, DEFAULT_LOG_LEVEL);
        if ((val instanceof Boolean)) {
            return "off";
        }
        return (getProperty(LOG_LEVEL, DEFAULT_LOG_LEVEL)).toLowerCase();
    }

    private CrossProcessConfig initCrossProcessConfig() {
        Boolean prop = getProperty("cross_application_tracing");
        Map props = nestedProps(CROSS_APPLICATION_TRACER);
        if (prop != null) {
            if (props == null) {
                props = createMap();
            }
            props.put("cross_application_tracing", prop);
        }
        return CrossProcessConfigImpl.createCrossProcessConfig(props);
    }

    private StripExceptionConfig initStripExceptionConfig(boolean highSecurity) {
        Map props = nestedProps(STRIP_EXCEPTION_MESSAGES);
        return StripExceptionConfigImpl.createStripExceptionConfig(props, highSecurity);
    }

    private ThreadProfilerConfig initThreadProfilerConfig() {
        Map props = nestedProps(THREAD_PROFILER);
        return ThreadProfilerConfigImpl.createThreadProfilerConfig(props);
    }

    private KeyTransactionConfig initKeyTransactionConfig(long apdexTInMillis) {
        Map props = nestedProps(KEY_TRANSACTIONS);
        return KeyTransactionConfigImpl.createKeyTransactionConfig(props, apdexTInMillis);
    }

    private TransactionTracerConfigImpl initTransactionTracerConfig(long apdexTInMillis, boolean highSecurity) {
        Map props = nestedProps(TRANSACTION_TRACER);
        return TransactionTracerConfigImpl.createTransactionTracerConfig(props, apdexTInMillis, highSecurity);
    }

    private ErrorCollectorConfig initErrorCollectorConfig() {
        Map props = nestedProps(ERROR_COLLECTOR);
        return ErrorCollectorConfigImpl.createErrorCollectorConfig(props);
    }

    private SqlTraceConfig initSqlTraceConfig() {
        Map props = nestedProps(SLOW_SQL);
        return SqlTraceConfigImpl.createSqlTraceConfig(props);
    }

    private JmxConfig initJmxConfig() {
        Map props = nestedProps(JMX);
        return JmxConfigImpl.createJmxConfig(props);
    }

    private JarCollectorConfig initJarCollectorConfig() {
        Map props = nestedProps(JAR_COLLECTOR);
        return JarCollectorConfigImpl.createJarCollectorConfig(props);
    }

    private ReinstrumentConfig initReinstrumentConfig() {
        Map props = nestedProps(REINSTRUMENT);
        return ReinstrumentConfigImpl.createReinstrumentConfig(props);
    }

    private BrowserMonitoringConfig initBrowserMonitoringConfig() {
        Map props = nestedProps(BROWSER_MONITORING);
        return BrowserMonitoringConfigImpl.createBrowserMonitoringConfig(props);
    }

    private ClassTransformerConfig initClassTransformerConfig() {
        boolean customTracingEnabled = getProperty(ENABLE_CUSTOM_TRACING, DEFAULT_ENABLE_CUSTOM_TRACING);
        Map props = nestedProps(CLASS_TRANSFORMER);
        return ClassTransformerConfigImpl.createClassTransformerConfig(props, customTracingEnabled);
    }

    private CircuitBreakerConfig initCircuitBreakerConfig() {
        Map props = nestedProps("circuitbreaker");
        return new CircuitBreakerConfig(props);
    }

    public long getApdexTInMillis() {
        return apdexTInMillis;
    }

    public long getApdexTInMillis(String transactionName) {
        return keyTransactionConfig.getApdexTInMillis(transactionName);
    }

    public boolean isApdexTSet() {
        return isApdexTSet;
    }

    public boolean isApdexTSet(String transactionName) {
        return keyTransactionConfig.isApdexTSet(transactionName);
    }

    public boolean isAgentEnabled() {
        return enabled;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public String getProxyPassword() {
        return proxyPass;
    }

    public String getApiHost() {
        return getProperty(API_HOST, DEFAULT_API_HOST);
    }

    public int getApiPort() {
        return getProperty(API_PORT, isSSL ? DEFAULT_SSL_PORT : DEFAULT_PORT);
    }

    public boolean isSSL() {
        return isSSL;
    }

    public String getApplicationName() {
        return appName;
    }

    public List<String> getApplicationNames() {
        return appNames;
    }

    public boolean isCpuSamplingEnabled() {
        return cpuSamplingEnabled;
    }

    public boolean isAutoAppNamingEnabled() {
        return autoAppNamingEnabled;
    }

    public boolean isAutoTransactionNamingEnabled() {
        return autoTransactionNamingEnabled;
    }

    public boolean isDebugEnabled() {
        return debug;
    }

    public boolean isSessionCountTrackingEnabled() {
        return sessionCountTrackingEnabled;
    }

    public String getLanguage() {
        return getProperty(LANGUAGE, DEFAULT_LANGUAGE);
    }

    public boolean isSendDataOnExit() {
        return getProperty(SEND_DATA_ON_EXIT, DEFAULT_SEND_DATA_ON_EXIT);
    }

    public long getSendDataOnExitThresholdInMillis() {
        int valueInSecs = getIntProperty(SEND_DATA_ON_EXIT_THRESHOLD, DEFAULT_SEND_DATA_ON_EXIT_THRESHOLD);
        return TimeUnit.MILLISECONDS.convert(valueInSecs, TimeUnit.SECONDS);
    }

    public boolean isAuditMode() {
        return auditMode;
    }

    public boolean isReportSqlParserErrors() {
        return reportSqlParserErrors;
    }

    public int getTransactionSizeLimit() {
        return transactionSizeLimit;
    }

    public boolean waitForRPMConnect() {
        return waitForRPMConnect;
    }

    public boolean isSyncStartup() {
        return getProperty(SYNC_STARTUP, DEFAULT_SYNC_STARTUP);
    }

    public boolean isSendEnvironmentInfo() {
        return getProperty(SEND_ENVIRONMENT_INFO, DEFAULT_SEND_ENVIRONMENT_INFO);
    }

    public boolean isLoggingToStdOut() {
        String logFileName = getLogFileName();
        return STDOUT.equalsIgnoreCase(logFileName);
    }

    public int getLogFileCount() {
        return getIntProperty(LOG_FILE_COUNT, DEFAULT_LOG_FILE_COUNT);
    }

    public String getLogFileName() {
        return getProperty(LOG_FILE_NAME, DEFAULT_LOG_FILE_NAME);
    }

    public String getLogFilePath() {
        return getProperty(LOG_FILE_PATH);
    }

    public String getLogLevel() {
        return logLevel;
    }

    public int getLogLimit() {
        return getIntProperty(LOG_LIMIT, DEFAULT_LOG_LIMIT);
    }

    public TransactionTracerConfig getTransactionTracerConfig() {
        return transactionTracerConfig;
    }

    public TransactionTracerConfig getBackgroundTransactionTracerConfig() {
        return backgroundTransactionTracerConfig;
    }

    public TransactionTracerConfig getRequestTransactionTracerConfig() {
        return requestTransactionTracerConfig;
    }

    public ErrorCollectorConfig getErrorCollectorConfig() {
        return errorCollectorConfig;
    }

    public SqlTraceConfig getSqlTraceConfig() {
        return sqlTraceConfig;
    }

    public CrossProcessConfig getCrossProcessConfig() {
        return crossProcessConfig;
    }

    public ThreadProfilerConfig getThreadProfilerConfig() {
        return threadProfilerConfig;
    }

    public JmxConfig getJmxConfig() {
        return jmxConfig;
    }

    public JarCollectorConfig getJarCollectorConfig() {
        return jarCollectorConfig;
    }

    public ReinstrumentConfig getReinstrumentConfig() {
        return reinstrumentConfig;
    }

    public BrowserMonitoringConfig getBrowserMonitoringConfig() {
        return browserMonitoringConfig;
    }

    public ClassTransformerConfig getClassTransformerConfig() {
        return classTransformerConfig;
    }

    public List<String> getIgnoreJars() {
        return ignoreJars;
    }

    public boolean isSendJvmProps() {
        return sendJvmProps;
    }

    public boolean isUsePrivateSSL() {
        return usePrivateSSL;
    }

    public boolean isLogDaily() {
        return logDaily;
    }

    public boolean isXraySessionEnabled() {
        return xRaySessionsEnabled;
    }

    public boolean isTrimStats() {
        return trimStats;
    }

    public boolean isPlatformInformationEnabled() {
        return platformInformationEnabled;
    }

    public Set<String> getJDBCSupport() {
        return jdbcSupport;
    }

    public boolean isGenericJDBCSupportEnabled() {
        return genericJdbcSupportEnabled;
    }

    public int getMaxStackTraceLines() {
        return maxStackTraceLines;
    }

    public Config getInstrumentationConfig() {
        return instrumentationConfig;
    }

    public int getMaxUserParameters() {
        return MAX_USER_PARAMETERS;
    }

    public int getMaxUserParameterSize() {
        return MAX_USER_PARAMETER_SIZE;
    }

    public boolean isHighSecurity() {
        return highSecurity;
    }

    public boolean getIbmWorkaroundEnabled() {
        return ibmWorkaroundEnabled;
    }

    public LabelsConfig getLabelsConfig() {
        return labelsConfig;
    }

    public boolean isStartupTimingEnabled() {
        return startupTimingEnabled;
    }

    public CircuitBreakerConfig getCircuitBreakerConfig() {
        return circuitBreakerConfig;
    }

    public StripExceptionConfig getStripExceptionConfig() {
        return stripExceptionConfig;
    }
}