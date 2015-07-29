package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.deps.org.objectweb.asm.commons.Method;

public class MethodWithAccess
{
  protected final boolean isStatic;
  protected final Method method;

  public MethodWithAccess(boolean isStatic, Method method)
  {
    this.method = method;
    this.isStatic = isStatic;
  }

  public boolean isStatic() {
    return this.isStatic;
  }

  public Method getMethod() {
    return this.method;
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    if ((o == null) || (getClass() != o.getClass())) {
      return false;
    }
    MethodWithAccess that = (MethodWithAccess)o;

    if (this.isStatic != that.isStatic)
      return false;
    if (this.method != null ? !this.method.equals(that.method) : that.method != null) {
      return false;
    }
    return true;
  }

  public int hashCode()
  {
    int result = this.isStatic ? 1 : 0;
    result = 31 * result + (this.method != null ? this.method.hashCode() : 0);
    return result;
  }

  public String toString()
  {
    return this.isStatic ? "static " + this.method : this.method.toString();
  }
}