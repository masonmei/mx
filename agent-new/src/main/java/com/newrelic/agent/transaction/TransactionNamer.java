package com.newrelic.agent.transaction;

public abstract interface TransactionNamer
{
  public abstract void setTransactionName();
}