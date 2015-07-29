package com.newrelic.agent.extension;

import com.newrelic.deps.org.yaml.snakeyaml.constructor.Construct;

public abstract class ConfigurationConstruct implements Construct {
    private final String name;

    public ConfigurationConstruct(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}