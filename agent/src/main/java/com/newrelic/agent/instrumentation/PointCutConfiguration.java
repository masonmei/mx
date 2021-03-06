package com.newrelic.agent.instrumentation;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.BaseConfig;
import com.newrelic.agent.config.Config;
import com.newrelic.agent.service.ServiceFactory;

public class PointCutConfiguration {
    private final String name;
    private final String groupName;
    private final Config config;
    private final boolean enabledByDefault;

    public PointCutConfiguration(Class<? extends PointCut> pc) {
        this(pc.getName(), null, true);
    }

    public PointCutConfiguration(String configurationName) {
        this(configurationName, null, true);
    }

    public PointCutConfiguration(String configurationName, boolean enabledByDefault) {
        this(configurationName, null, enabledByDefault);
    }

    public PointCutConfiguration(String configurationName, String configurationGroupName, boolean enabledByDefault) {
        name = configurationName;
        groupName = configurationGroupName;
        this.enabledByDefault = enabledByDefault;
        config = initConfig(configurationName);
    }

    public final String getName() {
        return name;
    }

    public final String getGroupName() {
        return groupName;
    }

    public Config getConfiguration() {
        return config;
    }

    private Config initConfig(String name) {
        Map config = Collections.emptyMap();
        if (name != null) {
            Object pointCutConfig =
                    ServiceFactory.getConfigService().getDefaultAgentConfig().getClassTransformerConfig()
                            .getProperty(name);

            if ((pointCutConfig instanceof Map)) {
                config = (Map) pointCutConfig;
            }
        }
        return new BaseConfig(config);
    }

    protected final Config getGroupConfig() {
        return initConfig(groupName);
    }

    public boolean isEnabled() {
        if (!((Boolean) getGroupConfig().getProperty("enabled", Boolean.valueOf(true))).booleanValue()) {
            String msg = MessageFormat.format("Disabled point cut \"{0}\" (\"{1}\" group)",
                                                     new Object[] {getName(), getGroupName()});
            Agent.LOG.info(msg);
            return false;
        }
        Config pointCutConfig = getConfiguration();
        boolean val =
                ((Boolean) pointCutConfig.getProperty("enabled", Boolean.valueOf(isEnabledByDefault()))).booleanValue();
        if (val != isEnabledByDefault()) {
            String msg =
                    MessageFormat.format("{0}abled point cut \"{1}\"", new Object[] {val ? "En" : "Dis", getName()});
            Agent.LOG.info(msg);
        }
        return val;
    }

    protected boolean isEnabledByDefault() {
        return enabledByDefault;
    }
}