package com.newrelic.agent.tracers.jasper;

public abstract interface TemplateText {
    public abstract String getText() throws Exception;

    public abstract void setText(String paramString) throws Exception;
}