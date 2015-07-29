package com.newrelic.agent.config;

public class SystemPropertyFactory {
    private static volatile SystemPropertyProvider SYSTEM_PROPERTY_PROVIDER = new SystemPropertyProvider();

    public static SystemPropertyProvider getSystemPropertyProvider() {
        return SYSTEM_PROPERTY_PROVIDER;
    }

    public static void setSystemPropertyProvider(SystemPropertyProvider provider) {
        SYSTEM_PROPERTY_PROVIDER = provider;
    }
}