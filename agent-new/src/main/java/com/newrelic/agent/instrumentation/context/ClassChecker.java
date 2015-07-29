package com.newrelic.agent.instrumentation.context;

public abstract interface ClassChecker
{
  public abstract void check(byte[] paramArrayOfByte);
}