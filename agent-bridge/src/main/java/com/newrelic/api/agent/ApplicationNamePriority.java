package com.newrelic.api.agent;

public enum ApplicationNamePriority {
    NONE,
    CONTEXT_PATH,
    CONTEXT_NAME,
    CONTEXT_PARAM,
    FILTER_INIT_PARAM,
    SERVLET_INIT_PARAM,
    REQUEST_ATTRIBUTE;

    private ApplicationNamePriority() {
    }
}