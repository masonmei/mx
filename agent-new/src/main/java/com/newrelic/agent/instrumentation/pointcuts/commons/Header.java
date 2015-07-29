package com.newrelic.agent.instrumentation.pointcuts.commons;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName = {"com/newrelic/agent/deps/org/apache/http/Header",
                                            "com/newrelic/agent/deps/org/apache/commons/httpclient/Header"})
public abstract interface Header {
    public abstract String getValue();
}