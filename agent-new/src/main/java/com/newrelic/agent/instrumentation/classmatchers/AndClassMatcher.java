package com.newrelic.agent.instrumentation.classmatchers;

import com.newrelic.deps.org.objectweb.asm.ClassReader;
import java.util.Arrays;
import java.util.Collection;

public class AndClassMatcher extends ManyClassMatcher
{
  public AndClassMatcher(ClassMatcher[] matchers)
  {
    this(Arrays.asList(matchers));
  }

  public AndClassMatcher(Collection<ClassMatcher> matchers) {
    super(matchers);
  }

  public static ClassMatcher getClassMatcher(ClassMatcher[] classMatchers) {
    if (classMatchers.length == 0)
      return new NoMatchMatcher();
    if (classMatchers.length == 1) {
      return classMatchers[0];
    }
    return new AndClassMatcher(classMatchers);
  }

  public boolean isMatch(ClassLoader loader, ClassReader cr)
  {
    for (ClassMatcher matcher : getClassMatchers()) {
      if (!matcher.isMatch(loader, cr)) {
        return false;
      }
    }
    return true;
  }

  public boolean isMatch(Class<?> clazz)
  {
    for (ClassMatcher matcher : getClassMatchers()) {
      if (!matcher.isMatch(clazz)) {
        return false;
      }
    }
    return true;
  }
}