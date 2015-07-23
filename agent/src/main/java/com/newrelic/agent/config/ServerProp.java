package com.newrelic.agent.config;

public class ServerProp {
    private final Object value;

    private ServerProp(Object value) {
        this.value = value;
    }

    public static ServerProp createPropObject(Object value) {
        return new ServerProp(value);
    }

    public Object getValue() {
        return value;
    }
}