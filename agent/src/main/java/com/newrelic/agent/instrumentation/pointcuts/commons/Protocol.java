package com.newrelic.agent.instrumentation.pointcuts.commons;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName = {"com/newrelic/deps/org/apache/commons/httpclient/protocol/Protocol"})
public interface Protocol {
    String getScheme();
}