package com.newrelic.agent.instrumentation.pointcuts.commons;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMapper;
import com.newrelic.agent.instrumentation.pointcuts.MethodMapper;

@InterfaceMapper(originalInterfaceName="com/newrelic/agent/deps/org/apache/commons/httpclient/HttpMethod", className={"com/newrelic/agent/deps/org/apache/commons/httpclient/HttpMethodBase"})
public abstract interface HttpMethodExtension
{
  @MethodMapper(originalMethodName="getRequestHeader", originalDescriptor="(Ljava/lang/String;)Lorg/apache/commons/httpclient/Header;", invokeInterface=false)
  public abstract Object _nr_getRequestHeader(String paramString);

  @MethodMapper(originalMethodName="getResponseHeader", originalDescriptor="(Ljava/lang/String;)Lorg/apache/commons/httpclient/Header;", invokeInterface=false)
  public abstract Object _nr_getResponseHeader(String paramString);

  @MethodMapper(originalMethodName="getURI", originalDescriptor="()Lorg/apache/commons/httpclient/URI;", invokeInterface=false)
  public abstract Object _nr_getUri();
}