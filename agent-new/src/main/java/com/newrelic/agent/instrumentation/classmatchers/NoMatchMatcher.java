package com.newrelic.agent.instrumentation.classmatchers;

import com.newrelic.deps.org.objectweb.asm.ClassReader;
import java.util.Collection;
import java.util.Collections;

public class NoMatchMatcher extends ClassMatcher
{
  public static final ClassMatcher MATCHER = new NoMatchMatcher();

  public boolean isMatch(ClassLoader loader, ClassReader cr)
  {
    return false;
  }

  public boolean isMatch(Class<?> clazz)
  {
    return false;
  }

  public Collection<String> getClassNames()
  {
    return Collections.emptyList();
  }

  public int hashCode()
  {
    return super.hashCode();
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
}