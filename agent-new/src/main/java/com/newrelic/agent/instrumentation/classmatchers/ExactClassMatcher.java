package com.newrelic.agent.instrumentation.classmatchers;

import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.agent.util.Strings;
import java.util.Arrays;
import java.util.Collection;

public class ExactClassMatcher extends ClassMatcher
{
  private final Type type;
  private final String className;
  private final String internalName;

  public ExactClassMatcher(String className)
  {
    this.type = Type.getObjectType(Strings.fixInternalClassName(className));
    this.className = this.type.getClassName();
    this.internalName = this.type.getInternalName();
  }

  public boolean isMatch(ClassLoader loader, ClassReader cr)
  {
    return cr.getClassName().equals(this.internalName);
  }

  public boolean isMatch(Class<?> clazz)
  {
    return clazz.getName().equals(this.className);
  }

  public static ClassMatcher or(String[] classNames) {
    return OrClassMatcher.createClassMatcher(classNames);
  }

  public String getInternalClassName() {
    return this.internalName;
  }

  public boolean isExactClassMatcher()
  {
    return true;
  }

  public int hashCode()
  {
    int prime = 31;
    int result = 1;
    result = 31 * result + (this.type == null ? 0 : this.type.hashCode());
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
    ExactClassMatcher other = (ExactClassMatcher)obj;
    if (this.type == null) {
      if (other.type != null)
        return false;
    } else if (!this.type.equals(other.type))
      return false;
    return true;
  }

  public String toString()
  {
    return "ExactClassMatcher(" + this.internalName + ")";
  }

  public Collection<String> getClassNames()
  {
    return Arrays.asList(new String[] { this.internalName });
  }
}