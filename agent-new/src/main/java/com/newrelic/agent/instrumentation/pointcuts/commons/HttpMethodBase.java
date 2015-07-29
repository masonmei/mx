package com.newrelic.agent.instrumentation.pointcuts.commons;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName={"com/newrelic/agent/deps/org/apache/commons/httpclient/HttpMethodBase"})
public abstract interface HttpMethodBase
{
  public abstract void setRequestHeader(String paramString1, String paramString2);
}