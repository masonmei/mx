package com.newrelic.agent.instrumentation.pointcuts.frameworks.spring;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMapper;
import com.newrelic.agent.instrumentation.pointcuts.MethodMapper;
import java.lang.reflect.Method;

@InterfaceMapper(className={"org/springframework/web/method/HandlerMethod"}, originalInterfaceName="org/springframework/web/method/HandlerMethod")
public abstract interface HandlerMethod
{
  @MethodMapper(originalMethodName="getBridgedMethod", originalDescriptor="()Ljava/lang/reflect/Method;", invokeInterface=false)
  public abstract Method _nr_getBridgedMethod();

  @MethodMapper(originalMethodName="getBean", originalDescriptor="()Ljava/lang/Object;", invokeInterface=false)
  public abstract Object _nr_getBean();
}