package com.newrelic.agent.instrumentation.pointcuts.commons;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName = {"com/newrelic/deps/org/apache/commons/httpclient/URI"})
public interface URI {
    String getScheme();

    String getHost();

    int getPort();

    String getPath();
}