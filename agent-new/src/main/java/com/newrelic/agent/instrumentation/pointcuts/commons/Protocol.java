package com.newrelic.agent.instrumentation.pointcuts.commons;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName={"com/newrelic/agent/deps/org/apache/commons/httpclient/protocol/Protocol"})
public abstract interface Protocol
{
  public abstract String getScheme();
}