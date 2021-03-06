package com.newrelic.agent.instrumentation.pointcuts.commons;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName = {"com/newrelic/deps/org/apache/http/Header",
                                            "com/newrelic/deps/org/apache/commons/httpclient/Header"})
public interface Header {
    String getValue();
}