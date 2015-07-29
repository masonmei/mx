package com.newrelic.agent.instrumentation.pointcuts.commons;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName = {"com/newrelic/deps/org/apache/commons/httpclient/NameValuePair"})
public interface NameValuePair {
    String getValue();
}