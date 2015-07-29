package com.newrelic.agent.instrumentation.methodmatchers;

import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.Method;
import java.util.Set;

public class ExactReturnTypeMethodMatcher
  implements MethodMatcher
{
  private final Type returnType;

  public ExactReturnTypeMethodMatcher(Type returnType)
  {
    this.returnType = returnType;
  }

  public boolean matches(int access, String name, String desc, Set<String> annotations)
  {
    return Type.getReturnType(desc).equals(this.returnType);
  }

  public Method[] getExactMethods()
  {
    return null;
  }
}