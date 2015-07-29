package com.newrelic.agent.instrumentation.pointcuts.container.jetty;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;
import java.util.List;

@InterfaceMixin(originalClassName={"org/eclipse/jetty/util/MultiException"})
public abstract interface MultiException
{
  public abstract List<Throwable> getThrowables();
}