package com.newrelic.agent.instrumentation.pointcuts.play2;

import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName={"play/core/Router$Routes$TaggingInvoker"})
public abstract interface TaggingInvoker
{
  @FieldAccessor(fieldName="handlerDef")
  public abstract Object getHandlerDef();

  @FieldAccessor(fieldName="handlerDef")
  public abstract void setHandlerDef(Object paramObject);
}