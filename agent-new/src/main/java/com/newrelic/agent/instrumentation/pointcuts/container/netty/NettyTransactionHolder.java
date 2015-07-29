package com.newrelic.agent.instrumentation.pointcuts.container.netty;

import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;
import com.newrelic.agent.instrumentation.pointcuts.TransactionHolder;

@InterfaceMixin(originalClassName={"org/jboss/netty/channel/socket/nio/NioAcceptedSocketChannel"})
public abstract interface NettyTransactionHolder extends TransactionHolder
{
  public static final String CLASS = "org/jboss/netty/channel/socket/nio/NioAcceptedSocketChannel";

  @FieldAccessor(fieldName="transaction", volatileAccess=true)
  public abstract void _nr_setTransaction(Object paramObject);

  @FieldAccessor(fieldName="transaction", volatileAccess=true)
  public abstract Object _nr_getTransaction();

  @FieldAccessor(fieldName="name", volatileAccess=true)
  public abstract void _nr_setName(Object paramObject);

  @FieldAccessor(fieldName="name", volatileAccess=true)
  public abstract Object _nr_getName();
}