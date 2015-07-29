package com.newrelic.agent.instrumentation.methodmatchers;

import com.newrelic.deps.org.objectweb.asm.commons.Method;
import java.util.Set;

public class NameMethodMatcher
  implements MethodMatcher
{
  private final String name;

  public NameMethodMatcher(String name)
  {
    this.name = name;
  }

  public boolean matches(int access, String name, String desc, Set<String> annotations)
  {
    return this.name.equals(name);
  }

  public String toString()
  {
    return "Match " + this.name;
  }

  public int hashCode()
  {
    int prime = 31;
    int result = 1;
    result = 31 * result + (this.name == null ? 0 : this.name.hashCode());
    return result;
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
    NameMethodMatcher other = (NameMethodMatcher)obj;
    if (this.name == null) {
      if (other.name != null)
        return false;
    }
    else if (!this.name.equals(other.name)) {
      return false;
    }
    return true;
  }

  public Method[] getExactMethods()
  {
    return null;
  }
}