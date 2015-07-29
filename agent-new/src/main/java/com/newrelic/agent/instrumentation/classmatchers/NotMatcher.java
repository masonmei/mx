package com.newrelic.agent.instrumentation.classmatchers;

import com.newrelic.deps.org.objectweb.asm.ClassReader;
import java.util.Collection;
import java.util.Collections;

public class NotMatcher extends ClassMatcher
{
  private final ClassMatcher matcher;

  public NotMatcher(ClassMatcher notMatch)
  {
    this.matcher = notMatch;
  }

  public boolean isMatch(ClassLoader loader, ClassReader cr)
  {
    return !this.matcher.isMatch(loader, cr);
  }

  public boolean isMatch(Class<?> clazz)
  {
    return !this.matcher.isMatch(clazz);
  }

  public int hashCode()
  {
    int prime = 31;
    int result = 1;
    result = 31 * result + (this.matcher == null ? 0 : this.matcher.hashCode());
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
    NotMatcher other = (NotMatcher)obj;
    if (this.matcher == null) {
      if (other.matcher != null)
        return false;
    }
    else if (!this.matcher.equals(other.matcher)) {
      return false;
    }
    return true;
  }

  public Collection<String> getClassNames()
  {
    return Collections.emptyList();
  }
}