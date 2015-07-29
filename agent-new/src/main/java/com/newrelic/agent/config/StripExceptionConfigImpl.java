package com.newrelic.agent.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StripExceptionConfigImpl extends BaseConfig implements StripExceptionConfig {
    public static final String ENABLED = "enabled";
    public static final String WHITELIST = "whitelist";
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.strip_exception_messages.";
    private final boolean isEnabled;
    private final Set<String> whitelist;

    private StripExceptionConfigImpl(Map<String, Object> props, boolean highSecurity) {
        super(props, "newrelic.config.strip_exception_messages.");
        this.isEnabled = ((Boolean) getProperty("enabled", Boolean.valueOf(highSecurity))).booleanValue();
        this.whitelist = Collections.unmodifiableSet(new HashSet(getUniqueStrings("whitelist")));
    }

    static StripExceptionConfig createStripExceptionConfig(Map<String, Object> settings, boolean highSecurity) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new StripExceptionConfigImpl(settings, highSecurity);
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public Set<String> getWhitelist() {
        return this.whitelist;
    }
}