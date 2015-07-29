package com.newrelic.agent.instrumentation.methodmatchers;

import com.newrelic.deps.com.google.common.collect.ImmutableSet;
import com.newrelic.deps.org.objectweb.asm.commons.Method;
import java.util.Set;

public abstract interface MethodMatcher
{
  public static final Set<String> UNSPECIFIED_ANNOTATIONS = ImmutableSet.of();
  public static final int UNSPECIFIED_ACCESS = -1;

  public abstract boolean matches(int paramInt, String paramString1, String paramString2, Set<String> paramSet);

  public abstract boolean equals(Object paramObject);

  public abstract Method[] getExactMethods();
}