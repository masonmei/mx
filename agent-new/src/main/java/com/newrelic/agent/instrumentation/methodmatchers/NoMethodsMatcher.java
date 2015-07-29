package com.newrelic.agent.instrumentation.methodmatchers;

import com.newrelic.deps.org.objectweb.asm.commons.Method;
import java.util.Set;

public final class NoMethodsMatcher
  implements MethodMatcher
{
  public boolean matches(int access, String name, String desc, Set<String> annotations)
  {
    return false;
  }

  public boolean equals(Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    return true;
  }

  public int hashCode()
  {
    return super.hashCode();
  }

  public Method[] getExactMethods()
  {
    return null;
  }
}