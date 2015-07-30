package com.newrelic.agent.extension;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.newrelic.agent.Agent;
import com.newrelic.agent.config.BaseConfig;
import com.newrelic.agent.config.Config;
import com.newrelic.agent.config.PointCutConfig;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.jmx.create.JmxConfiguration;
import com.newrelic.agent.jmx.create.JmxYmlParser;

public class YamlExtension extends Extension {
    private final Config configuration;
    private final boolean enabled;

    public YamlExtension(ClassLoader classloader, String name, Map<String, Object> configuration, boolean custom)
            throws IllegalArgumentException {
        super(classloader, name, custom);
        if (name == null) {
            throw new IllegalArgumentException("Extensions must have a name");
        }
        this.configuration = new BaseConfig(configuration);
        enabled = this.configuration.getProperty("enabled", true);
    }

    YamlExtension(ClassLoader classloader, Map<String, Object> config, boolean custom) {
        this(classloader, (String) config.get("name"), config, custom);
    }

    public String toString() {
        return getName() + " Extension";
    }

    public final Config getConfiguration() {
        return configuration;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getVersion() {
        return (String) configuration.getProperty("version", "n/a");
    }

    public double getVersionNumber() {
        try {
            return ((Double) configuration.getProperty("version", Double.valueOf(0.0D))).doubleValue();
        } catch (Exception e) {
            Agent.LOG.severe(MessageFormat.format("Extension \"{0}\" has an invalid version number: {1}: {2}",
                                                         new Object[] {getName(), e.getClass().getSimpleName(),
                                                                              e.getMessage()}));
        }
        return 0.0D;
    }

    public Collection<JmxConfiguration> getJmxConfig() {
        Object jmx = getConfiguration().getProperty("jmx");
        if ((jmx != null) && ((jmx instanceof List))) {
            List list = Lists.newArrayList();
            for (Map config : (List<Map>) jmx) {
                list.add(new JmxYmlParser(config));
            }
            return list;
        }
        return Collections.emptyList();
    }

    public Collection<ExtensionClassAndMethodMatcher> getInstrumentationMatchers() {
        if (isEnabled()) {
            Object instrumentation = getConfiguration().getProperty("instrumentation");
            if ((instrumentation instanceof Map)) {
                return PointCutConfig.getExtensionPointCuts(this, (Map) instrumentation);
            }
            if (configuration.getProperty("jmx", null) == null) {
                String msg = MessageFormat
                                     .format("Extension {0} either does not have an instrumentation section or has an"
                                                     + " invalid instrumentation section. Please check the format of "
                                                     + "the file.", new Object[] {getName()});

                Agent.LOG.severe(msg);
            }
        }
        return Collections.emptyList();
    }
}