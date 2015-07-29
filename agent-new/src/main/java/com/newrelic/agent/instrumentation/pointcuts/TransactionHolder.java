package com.newrelic.agent.instrumentation.pointcuts;

@LoadOnBootstrap
public abstract interface TransactionHolder
{
  public abstract Object _nr_getTransaction();

  public abstract void _nr_setTransaction(Object paramObject);

  public abstract Object _nr_getName();

  public abstract void _nr_setName(Object paramObject);
}