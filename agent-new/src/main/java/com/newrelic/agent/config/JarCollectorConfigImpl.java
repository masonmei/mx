package com.newrelic.agent.config;

import java.util.Collections;
import java.util.Map;

public class JarCollectorConfigImpl extends BaseConfig implements JarCollectorConfig {
    public static final Integer DEFAULT_MAX_CLASS_LOADERS = Integer.valueOf(5000);
    public static final String ENABLED = "enabled";
    public static final String MAX_CLASS_LOADERS = "max_class_loaders";
    public static final Boolean DEFAULT_ENABLED = Boolean.TRUE;
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.module.";
    private final boolean isEnabled;
    private final int maxClassLoaders;

    public JarCollectorConfigImpl(Map<String, Object> pProps) {
        super(pProps, "newrelic.config.module.");
        this.isEnabled = ((Boolean) getProperty("enabled", DEFAULT_ENABLED)).booleanValue();
        this.maxClassLoaders = ((Integer) getProperty("max_class_loaders", DEFAULT_MAX_CLASS_LOADERS)).intValue();
    }

    static JarCollectorConfigImpl createJarCollectorConfig(Map<String, Object> settings) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new JarCollectorConfigImpl(settings);
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public int getMaxClassLoaders() {
        return this.maxClassLoaders;
    }
}