package com.newrelic.agent.instrumentation.pointcuts.commons;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName = {"com/newrelic/deps/org/apache/commons/httpclient/HttpMethodBase"})
public interface HttpMethodBase {
    void setRequestHeader(String paramString1, String paramString2);
}