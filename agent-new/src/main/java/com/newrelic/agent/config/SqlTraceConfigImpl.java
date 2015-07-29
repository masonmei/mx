package com.newrelic.agent.config;

import java.util.Collections;
import java.util.Map;

final class SqlTraceConfigImpl extends BaseConfig implements SqlTraceConfig {
    public static final String ENABLED = "enabled";
    public static final boolean DEFAULT_ENABLED = true;
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.slow_sql.";
    private final boolean isEnabled;

    private SqlTraceConfigImpl(Map<String, Object> props) {
        super(props, "newrelic.config.slow_sql.");
        this.isEnabled = ((Boolean) getProperty("enabled", Boolean.valueOf(true))).booleanValue();
    }

    static SqlTraceConfig createSqlTraceConfig(Map<String, Object> settings) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new SqlTraceConfigImpl(settings);
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }
}