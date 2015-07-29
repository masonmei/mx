package com.newrelic.agent.instrumentation.classmatchers;

import com.newrelic.deps.org.objectweb.asm.ClassReader;
import java.util.Collection;
import java.util.Collections;

public class AllClassesMatcher extends ClassMatcher
{
  public boolean isMatch(ClassLoader loader, ClassReader cr)
  {
    return (cr.getAccess() & 0x200) == 0;
  }

  public boolean isMatch(Class<?> clazz)
  {
    return !clazz.isInterface();
  }

  public Collection<String> getClassNames()
  {
    return Collections.emptyList();
  }
}