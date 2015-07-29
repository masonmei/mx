package com.newrelic.agent.instrumentation.pointcuts.container.netty;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;
import java.util.List;
import java.util.Map.Entry;

@InterfaceMixin(originalClassName={"org/jboss/netty/handler/codec/http/DefaultHttpRequest"})
public interface NettyHttpRequest
{
  String getUri();

  List<String> getHeaders(String paramString);

  List<Entry<String, String>> getHeaders();
}