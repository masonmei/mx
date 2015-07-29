package com.newrelic.agent.instrumentation.pointcuts.play2;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName={"play/core/Router$HandlerDef"})
public abstract interface HandlerDef
{
  public static final String CLASS_NAME = "play/core/Router$HandlerDef";

  public abstract String controller();

  public abstract String method();
}