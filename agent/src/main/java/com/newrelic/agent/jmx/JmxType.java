package com.newrelic.agent.jmx;

public enum JmxType {
    SIMPLE("simple"),

    MONOTONICALLY_INCREASING("monotonically_increasing");

    private String ymlName;

    private JmxType(String pYmlName) {
        ymlName = pYmlName;
    }

    public String getYmlName() {
        return ymlName;
    }
}