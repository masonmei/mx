package com.newrelic.agent.config;

import java.util.Map;

public class CircuitBreakerConfig extends BaseConfig {
    public static final String ENABLED = "enabled";
    public static final Boolean DEFAULT_ENABLED = Boolean.TRUE;
    public static final String MEMORY_THRESHOLD = "memory_threshold";
    public static final int DEFAULT_MEMORY_THRESHOLD = 20;
    public static final String GC_CPU_THRESHOLD = "gc_cpu_threshold";
    public static final int DEFAULT_GC_CPU_THRESHOLD = 10;
    public static final String PROPERTY_NAME = "circuitbreaker";
    public static final String PROPERTY_ROOT = "newrelic.config.circuitbreaker.";
    private boolean isEnabled;
    private int memoryThreshold;
    private int gcCpuThreshold;

    public CircuitBreakerConfig(Map<String, Object> pProps) {
        super(pProps, "newrelic.config.circuitbreaker.");
        isEnabled = ((Boolean) getProperty("enabled", DEFAULT_ENABLED)).booleanValue();
        memoryThreshold = ((Integer) getProperty("memory_threshold", Integer.valueOf(20))).intValue();
        gcCpuThreshold = ((Integer) getProperty("gc_cpu_threshold", Integer.valueOf(10))).intValue();
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public int getMemoryThreshold() {
        return memoryThreshold;
    }

    public int getGcCpuThreshold() {
        return gcCpuThreshold;
    }

    public boolean updateThresholds(int newGCCpuThreshold, int newMemoryThreshold) {
        if ((newGCCpuThreshold >= 0) && (newMemoryThreshold >= 0)) {
            gcCpuThreshold = newGCCpuThreshold;
            memoryThreshold = newMemoryThreshold;
            return true;
        }
        return false;
    }

    public boolean updateEnabled(boolean newEnabled) {
        if (isEnabled != newEnabled) {
            isEnabled = newEnabled;
            return true;
        }
        return false;
    }
}