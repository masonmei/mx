package com.newrelic.agent.tracers.jasper;

public abstract interface Node {
    public abstract Node getParent() throws Exception;

    public abstract String getQName() throws Exception;
}