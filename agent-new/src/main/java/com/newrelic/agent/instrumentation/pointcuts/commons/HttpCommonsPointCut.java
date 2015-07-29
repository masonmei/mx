package com.newrelic.agent.instrumentation.pointcuts.commons;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMapper;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;
import com.newrelic.agent.instrumentation.pointcuts.MethodMapper;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ExternalComponentPointCut;
import com.newrelic.agent.tracers.Tracer;

public abstract class HttpCommonsPointCut extends ExternalComponentPointCut
{
  public HttpCommonsPointCut(Class<? extends HttpCommonsPointCut> pointCutClass, ClassMatcher classMatcher, MethodMatcher methodMatcher)
  {
    super(new PointCutConfiguration(pointCutClass), classMatcher, methodMatcher);
  }

  public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object, String host, String uri, String methodName)
  {
    return new HttpCommonsTracer(transaction, sig, object, host, "CommonsHttp", uri, methodName);
  }

  @InterfaceMixin(originalClassName={"com/newrelic/agent/deps/org/apache/http/StatusLine"})
  public static abstract interface StatusLine
  {
    public abstract int getStatusCode();
  }

  @InterfaceMapper(originalInterfaceName="com/newrelic/agent/deps/org/apache/http/message/BasicHttpResponse", className={"com/newrelic/agent/deps/org/apache/http/message/BasicHttpResponse"})
  public static abstract interface BasicHttpResponseExtension
  {
    @MethodMapper(originalMethodName="getStatusLine", originalDescriptor="()Lorg/apache/http/StatusLine;", invokeInterface=false)
    public abstract Object _nr_getStatusLine();
  }
}