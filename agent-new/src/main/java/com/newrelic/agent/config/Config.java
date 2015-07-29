package com.newrelic.agent.config;

public abstract interface Config {
    public abstract <T> T getProperty(String paramString);

    public abstract <T> T getProperty(String paramString, T paramT);
}