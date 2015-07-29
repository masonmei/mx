package com.newrelic.agent.extension;

import java.util.Collection;

import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.jmx.create.JmxConfiguration;

public abstract class Extension {
    private final String name;
    private final ClassLoader classloader;
    private final boolean custom;

    public Extension(ClassLoader classloader, String name, boolean custom) {
        if (name == null) {
            throw new IllegalArgumentException("Extensions must have a name");
        }
        this.classloader = classloader;
        this.name = name;
        this.custom = custom;
    }

    public final String getName() {
        return this.name;
    }

    public final ClassLoader getClassLoader() {
        return this.classloader;
    }

    public String toString() {
        return getName() + " Extension";
    }

    public boolean isCustom() {
        return this.custom;
    }

    public abstract boolean isEnabled();

    public abstract String getVersion();

    public abstract double getVersionNumber();

    public abstract Collection<JmxConfiguration> getJmxConfig();

    public abstract Collection<ExtensionClassAndMethodMatcher> getInstrumentationMatchers();
}