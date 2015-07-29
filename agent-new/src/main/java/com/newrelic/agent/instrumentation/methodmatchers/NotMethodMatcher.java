package com.newrelic.agent.instrumentation.methodmatchers;

import com.newrelic.deps.org.objectweb.asm.commons.Method;
import java.util.Set;

public final class NotMethodMatcher
  implements MethodMatcher
{
  private MethodMatcher methodMatcher;

  public NotMethodMatcher(MethodMatcher methodMatcher)
  {
    this.methodMatcher = methodMatcher;
  }

  public boolean matches(int access, String name, String desc, Set<String> annotations)
  {
    return !this.methodMatcher.matches(access, name, desc, annotations);
  }

  public Method[] getExactMethods()
  {
    return null;
  }

  public int hashCode()
  {
    int prime = 31;
    int result = 1;
    result = 31 * result + (this.methodMatcher == null ? 0 : this.methodMatcher.hashCode());
    return result;
  }

  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    NotMethodMatcher other = (NotMethodMatcher)obj;
    if (this.methodMatcher == null) {
      if (other.methodMatcher != null)
        return false;
    } else if (!this.methodMatcher.equals(other.methodMatcher))
      return false;
    return true;
  }
}