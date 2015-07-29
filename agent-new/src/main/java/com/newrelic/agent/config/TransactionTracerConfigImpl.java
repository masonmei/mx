package com.newrelic.agent.config;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.agent.Agent;

public final class TransactionTracerConfigImpl extends BaseConfig implements TransactionTracerConfig {
    public static final String BACKGROUND_CATEGORY_NAME = "background";
    public static final String REQUEST_CATEGORY_NAME = "request";
    public static final String APDEX_F = "apdex_f";
    public static final String CATEGORY = "category";
    public static final String CATEGORY_NAME = "name";
    public static final String COLLECT_TRACES = "collect_traces";
    public static final String ENABLED = "enabled";
    public static final String EXPLAIN_ENABLED = "explain_enabled";
    public static final String EXPLAIN_THRESHOLD = "explain_threshold";
    public static final String GC_TIME_ENABLED = "gc_time_enabled";
    public static final String INSERT_SQL_MAX_LENGTH = "insert_sql_max_length";
    public static final String LOG_SQL = "log_sql";
    public static final String MAX_EXPLAIN_PLANS = "max_explain_plans";
    public static final String MAX_STACK_TRACE = "max_stack_trace";
    public static final String OBFUSCATED_SQL_FIELDS = "obfuscated_sql_fields";
    public static final String RECORD_SQL = "record_sql";
    public static final String SEGMENT_LIMIT = "segment_limit";
    public static final String STACK_TRACE_THRESHOLD = "stack_trace_threshold";
    public static final String TOP_N = "top_n";
    public static final String TRANSACTION_THRESHOLD = "transaction_threshold";
    public static final String TAKE_LAST_STATUS = "take_last_status";
    public static final String STACK_BASED_NAMING = "stack_based_naming";
    public static final boolean DEFAULT_STACK_BASED_NAMING = false;
    public static final boolean DEFAULT_COLLECT_TRACES = false;
    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_EXPLAIN_ENABLED = true;
    public static final boolean DEFAULT_TAKE_LAST_STATUS = false;
    public static final double DEFAULT_EXPLAIN_THRESHOLD = 0.5D;
    public static final boolean DEFAULT_GC_TIME_ENABLED = false;
    public static final int DEFAULT_INSERT_SQL_MAX_LENGTH = 2000;
    public static final boolean DEFAULT_LOG_SQL = false;
    public static final int DEFAULT_MAX_EXPLAIN_PLANS = 20;
    public static final int DEFAULT_MAX_STACK_TRACE = 20;
    public static final String DEFAULT_RECORD_SQL = "obfuscated";
    public static final int DEFAULT_SEGMENT_LIMIT = 3000;
    public static final double DEFAULT_STACK_TRACE_THRESHOLD = 0.5D;
    public static final String DEFAULT_TRANSACTION_THRESHOLD = "apdex_f";
    public static final int DEFAULT_TOP_N = 20;
    public static final int APDEX_F_MULTIPLE = 4;
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.transaction_tracer.";
    public static final String CATEGORY_REQUEST_SYSTEM_PROPERTY_ROOT =
            "newrelic.config.transaction_tracer.category.request.";
    public static final String CATEGORY_BACKGROUND_SYSTEM_PROPERTY_ROOT =
            "newrelic.config.transaction_tracer.category.background.";
    protected final String inheritedFromSystemPropertyRoot;
    private final boolean isEnabled;
    private final boolean isExplainEnabled;
    private final boolean isLogSql;
    private final boolean takeLastStatus;
    private final String recordSql;
    private final double explainThreshold;
    private final double explainThresholdInNanos;
    private final double stackTraceThreshold;
    private final double stackTraceThresholdInNanos;
    private final long transactionThreshold;
    private final long transactionThresholdInNanos;
    private final int insertSqlMaxLength;
    private final boolean gcTimeEnabled;
    private final int maxStackTraces;
    private final int maxSegments;
    private final int maxExplainPlans;
    private final int topN;
    private final boolean stackBasedNamingEnabled;

    private TransactionTracerConfigImpl(String systemPropertyRoot, String inheritedFromSystemPropertyRoot,
                                        Map<String, Object> props, long apdexTInMillis, boolean highSecurity) {
        super(props, systemPropertyRoot);
        this.inheritedFromSystemPropertyRoot = inheritedFromSystemPropertyRoot;
        isEnabled = initEnabled();
        isExplainEnabled = ((Boolean) getProperty("explain_enabled", Boolean.valueOf(true))).booleanValue();
        isLogSql = ((Boolean) getProperty("log_sql", Boolean.valueOf(false))).booleanValue();
        takeLastStatus = ((Boolean) getProperty("take_last_status", Boolean.valueOf(false))).booleanValue();
        if (takeLastStatus) {
            Agent.LOG.log(Level.INFO, MessageFormat.format("The property {0} has been deprecated.",
                                                                  new Object[] {"take_last_status"}));
        }

        recordSql = initRecordSql(highSecurity, props).intern();
        explainThreshold = (getDoubleProperty("explain_threshold", 0.5D) * 1000.0D);
        explainThresholdInNanos = TimeUnit.NANOSECONDS.convert((long) explainThreshold, TimeUnit.MILLISECONDS);
        stackTraceThreshold = (getDoubleProperty("stack_trace_threshold", 0.5D) * 1000.0D);
        stackTraceThresholdInNanos = TimeUnit.NANOSECONDS.convert((long) stackTraceThreshold, TimeUnit.MILLISECONDS);
        transactionThreshold = initTransactionThreshold(apdexTInMillis);
        transactionThresholdInNanos = TimeUnit.NANOSECONDS.convert(transactionThreshold, TimeUnit.MILLISECONDS);
        insertSqlMaxLength = getIntProperty("insert_sql_max_length", 2000);
        gcTimeEnabled = ((Boolean) getProperty("gc_time_enabled", Boolean.valueOf(false))).booleanValue();
        maxStackTraces = getIntProperty("max_stack_trace", 20);
        maxSegments = getIntProperty("segment_limit", 3000);
        maxExplainPlans = getIntProperty("max_explain_plans", 20);
        topN = getIntProperty("top_n", 20);
        stackBasedNamingEnabled = ((Boolean) getProperty("stack_based_naming", Boolean.valueOf(false))).booleanValue();
    }

    static TransactionTracerConfigImpl createTransactionTracerConfig(Map<String, Object> settings, long apdexTInMillis,
                                                                     boolean highSecurity) {
        return createTransactionTracerConfigImpl(settings, apdexTInMillis, highSecurity);
    }

    private static TransactionTracerConfigImpl createTransactionTracerConfigImpl(Map<String, Object> settings,
                                                                                 long apdexTInMillis,
                                                                                 boolean highSecurity) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new TransactionTracerConfigImpl("newrelic.config.transaction_tracer.", null, settings, apdexTInMillis,
                                                      highSecurity);
    }

    private boolean initEnabled() {
        boolean isEnabled = ((Boolean) getProperty("enabled", Boolean.valueOf(true))).booleanValue();

        boolean canCollectTraces = ((Boolean) getProperty("collect_traces", Boolean.valueOf(false))).booleanValue();
        return (isEnabled) && (canCollectTraces);
    }

    protected String initRecordSql(boolean highSecurity, Map<String, Object> props) {
        Object val = getProperty("record_sql", "obfuscated");
        String output;
        if ((val instanceof Boolean)) {
            output = "off";
        } else {
            output = (getProperty("record_sql", "obfuscated")).toLowerCase();
            if (!getUniqueStrings("obfuscated_sql_fields").isEmpty()) {
                Agent.LOG
                        .log(Level.WARNING, "The {0} setting is no longer supported.  Full SQL obfuscation is enabled.",
                                    new Object[] {"obfuscated_sql_fields"});

                output = "obfuscated";
            }

        }

        if ((highSecurity) && (!"off".equals(output))) {
            output = "obfuscated";
        }
        return output;
    }

    private long initTransactionThreshold(long apdexTInMillis) {
        Object threshold = getProperty("transaction_threshold", "apdex_f");
        if ("apdex_f".equals(threshold)) {
            return apdexTInMillis * 4L;
        }
        Number transactionThreshold = (Number) getProperty("transaction_threshold");
        return (long) (transactionThreshold.doubleValue() * 1000.0D);
    }

    private Map<String, Object> initCategorySettings(String category) {
        Set<Map<String, Object>> categories = getMapSet("category");
        for (Map<String, Object> categoryProps : categories) {
            if (category.equals(categoryProps.get("name"))) {
                return mergeSettings(getProperties(), categoryProps);
            }
        }
        return getProperties();
    }

    private Map<String, Object> mergeSettings(Map<String, Object> localSettings, Map<String, Object> serverSettings) {
        Map mergedSettings = createMap();
        if (localSettings != null) {
            mergedSettings.putAll(localSettings);
        }
        if (serverSettings != null) {
            mergedSettings.putAll(serverSettings);
        }
        return mergedSettings;
    }

    protected String getInheritedSystemPropertyKey(String key) {
        return inheritedFromSystemPropertyRoot + key;
    }

    protected Object getPropertyFromSystemProperties(String name, Object defaultVal) {
        String key = getSystemPropertyKey(name);
        Object value = parseValue(SystemPropertyFactory.getSystemPropertyProvider().getSystemProperty(key));
        if (value != null) {
            return value;
        }

        String inheritedKey = getInheritedSystemPropertyKey(name);
        return inheritedKey == null ? null
                       : parseValue(SystemPropertyFactory.getSystemPropertyProvider().getSystemProperty(inheritedKey));
    }

    protected Object getPropertyFromSystemEnvironment(String name, Object defaultVal) {
        String key = getSystemPropertyKey(name);
        Object value = parseValue(SystemPropertyFactory.getSystemPropertyProvider().getEnvironmentVariable(key));
        if (value != null) {
            return value;
        }

        String inheritedKey = getInheritedSystemPropertyKey(name);
        return inheritedKey == null ? null : parseValue(SystemPropertyFactory.getSystemPropertyProvider()
                                                                .getEnvironmentVariable(inheritedKey));
    }

    public double getExplainThresholdInMillis() {
        return explainThreshold;
    }

    public double getExplainThresholdInNanos() {
        return explainThresholdInNanos;
    }

    public String getRecordSql() {
        return recordSql;
    }

    public double getStackTraceThresholdInMillis() {
        return stackTraceThreshold;
    }

    public double getStackTraceThresholdInNanos() {
        return stackTraceThresholdInNanos;
    }

    public long getTransactionThresholdInMillis() {
        return transactionThreshold;
    }

    public long getTransactionThresholdInNanos() {
        return transactionThresholdInNanos;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean isExplainEnabled() {
        return isExplainEnabled;
    }

    public int getMaxExplainPlans() {
        return maxExplainPlans;
    }

    public int getTopN() {
        return topN;
    }

    public boolean isLogSql() {
        return isLogSql;
    }

    public boolean isGCTimeEnabled() {
        return gcTimeEnabled;
    }

    public int getInsertSqlMaxLength() {
        return insertSqlMaxLength;
    }

    public int getMaxStackTraces() {
        return maxStackTraces;
    }

    public int getMaxSegments() {
        return maxSegments;
    }

    public boolean isStackBasedNamingEnabled() {
        return stackBasedNamingEnabled;
    }

    TransactionTracerConfigImpl createRequestTransactionTracerConfig(long apdexTInMillis, boolean highSecurity) {
        Map settings = initCategorySettings("request");
        return new TransactionTracerConfigImpl("newrelic.config.transaction_tracer.category.request.",
                                                      "newrelic.config.transaction_tracer.", settings, apdexTInMillis,
                                                      highSecurity);
    }

    TransactionTracerConfigImpl createBackgroundTransactionTracerConfig(long apdexTInMillis, boolean highSecurity) {
        Map settings = initCategorySettings("background");
        return new TransactionTracerConfigImpl("newrelic.config.transaction_tracer.category.background.",
                                                      "newrelic.config.transaction_tracer.", settings, apdexTInMillis,
                                                      highSecurity);
    }
}