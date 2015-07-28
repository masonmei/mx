package com.newrelic.agent.instrumentation.pointcuts.commons;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName = {"org/apache/http/Header", "org/apache/commons/httpclient/Header"})
public interface Header {
    String getValue();
}