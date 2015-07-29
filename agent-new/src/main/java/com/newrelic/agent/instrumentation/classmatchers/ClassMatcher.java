package com.newrelic.agent.instrumentation.classmatchers;

import com.newrelic.deps.org.objectweb.asm.ClassReader;
import java.io.IOException;
import java.util.Collection;

public abstract class ClassMatcher
{
  protected static final String JAVA_LANG_OBJECT_INTERNAL_NAME = "java/lang/Object";

  public abstract boolean isMatch(ClassLoader paramClassLoader, ClassReader paramClassReader);

  public abstract boolean isMatch(Class<?> paramClass);

  public abstract Collection<String> getClassNames();

  public boolean isExactClassMatcher()
  {
    return false;
  }

  public ClassMatcher getClassMatcher(ClassLoader classLoader)
    throws IOException
  {
    return this;
  }
}