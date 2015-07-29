package com.newrelic.agent.instrumentation.pointcuts.container.netty;

import java.util.List;
import java.util.Map.Entry;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName = {"org/jboss/netty/handler/codec/http/DefaultHttpRequest"})
public interface NettyHttpRequest {
    String getUri();

    List<String> getHeaders(String paramString);

    List<Entry<String, String>> getHeaders();
}