package com.newrelic.agent.bridge;

import com.newrelic.api.agent.Config;

public class NoOpConfig implements Config {
    public static final Config Instance = new NoOpConfig();

    public <T> T getValue(String prop) {
        return null;
    }

    public <T> T getValue(String key, T defaultVal) {
        return defaultVal;
    }
}