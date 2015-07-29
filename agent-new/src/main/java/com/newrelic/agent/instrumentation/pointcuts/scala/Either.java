package com.newrelic.agent.instrumentation.pointcuts.scala;

import com.newrelic.agent.instrumentation.pointcuts.LoadOnBootstrap;

@LoadOnBootstrap
public abstract interface Either
{
  public abstract Object get();
}