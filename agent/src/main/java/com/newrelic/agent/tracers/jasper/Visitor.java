package com.newrelic.agent.tracers.jasper;

public abstract interface Visitor {
    public abstract void writeScriptlet(String paramString) throws Exception;
}