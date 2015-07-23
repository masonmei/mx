package com.newrelic.agent.instrumentation.pointcuts.commons;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName = {"com/newrelic/agent/deps/org/apache/commons/httpclient/NameValuePair"})
public abstract interface NameValuePair {
    public abstract String getValue();
}