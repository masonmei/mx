package com.newrelic.agent.instrumentation.pointcuts.commons;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName={"com/newrelic/agent/deps/org/apache/commons/httpclient/URI"})
public abstract interface URI
{
  public abstract String getScheme();

  public abstract String getHost();

  public abstract int getPort();

  public abstract String getPath();
}