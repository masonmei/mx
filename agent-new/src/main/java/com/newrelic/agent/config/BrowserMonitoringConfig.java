package com.newrelic.agent.config;

import java.util.Set;

public abstract interface BrowserMonitoringConfig {
    public abstract boolean isAutoInstrumentEnabled();

    public abstract Set<String> getDisabledAutoPages();

    public abstract String getLoaderType();

    public abstract boolean isDebug();

    public abstract boolean isSslForHttp();

    public abstract boolean isSslForHttpSet();

    public abstract boolean isAllowMultipleFooters();
}