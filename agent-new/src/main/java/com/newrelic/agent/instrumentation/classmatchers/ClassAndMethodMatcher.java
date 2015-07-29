package com.newrelic.agent.instrumentation.classmatchers;

import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;

public abstract interface ClassAndMethodMatcher
{
  public abstract ClassMatcher getClassMatcher();

  public abstract MethodMatcher getMethodMatcher();
}