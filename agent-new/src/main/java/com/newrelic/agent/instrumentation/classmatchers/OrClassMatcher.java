package com.newrelic.agent.instrumentation.classmatchers;

import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.agent.util.Strings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class OrClassMatcher extends ManyClassMatcher
{
  public OrClassMatcher(ClassMatcher[] matchers)
  {
    this(Arrays.asList(matchers));
  }

  public OrClassMatcher(Collection<ClassMatcher> matchers) {
    super(getOptimizedClassMatchers(matchers));
  }

  private static Collection<ClassMatcher> getOptimizedClassMatchers(Collection<ClassMatcher> matchers)
  {
    Collection exactMatcherClassNames = new ArrayList(matchers.size());
    Collection otherMatchers = new LinkedList();
    for (ClassMatcher matcher : matchers) {
      if ((matcher instanceof ExactClassMatcher))
        exactMatcherClassNames.add(((ExactClassMatcher)matcher).getInternalClassName());
      else {
        otherMatchers.add(matcher);
      }
    }
    if (exactMatcherClassNames.size() <= 1) {
      return matchers;
    }

    otherMatchers.add(createClassMatcher((String[])exactMatcherClassNames.toArray(new String[exactMatcherClassNames.size()])));
    return otherMatchers;
  }

  public boolean isMatch(ClassLoader loader, ClassReader cr)
  {
    for (ClassMatcher matcher : getClassMatchers()) {
      if (matcher.isMatch(loader, cr)) {
        return true;
      }
    }
    return false;
  }

  public boolean isMatch(Class<?> clazz)
  {
    for (ClassMatcher matcher : getClassMatchers()) {
      if (matcher.isMatch(clazz)) {
        return true;
      }
    }
    return false;
  }

  public static ClassMatcher getClassMatcher(ClassMatcher[] classMatchers) {
    return getClassMatcher(Arrays.asList(classMatchers));
  }

  public static ClassMatcher getClassMatcher(Collection<ClassMatcher> classMatchers) {
    classMatchers = getOptimizedClassMatchers(classMatchers);
    if (classMatchers.size() == 0)
      return new NoMatchMatcher();
    if (classMatchers.size() == 1) {
      return (ClassMatcher)classMatchers.iterator().next();
    }
    return new OrClassMatcher(classMatchers);
  }

  static ClassMatcher createClassMatcher(String[] classNames)
  {
    if (classNames.length == 0)
      return NoMatchMatcher.MATCHER;
    if (classNames.length == 1) {
      return new ExactClassMatcher(classNames[0]);
    }
    return new StringOrClassMatcher(classNames);
  }

  private static class StringOrClassMatcher extends ClassMatcher {
    private final Set<String> internalClassNames;
    private final Set<String> classNames;

    public StringOrClassMatcher(String[] internalClassNames) {
      this.internalClassNames = new HashSet();
      for (String name : internalClassNames) {
        this.internalClassNames.add(Strings.fixInternalClassName(name));
      }
      this.classNames = new HashSet();
      for (String name : this.internalClassNames)
        this.classNames.add(name.replace('/', '.'));
    }

    public boolean isMatch(ClassLoader loader, ClassReader cr)
    {
      return this.internalClassNames.contains(cr.getClassName());
    }

    public boolean isMatch(Class<?> clazz)
    {
      return this.classNames.contains(clazz.getName());
    }

    public String toString()
    {
      return this.classNames.toString();
    }

    public int hashCode()
    {
      int prime = 31;
      int result = 1;
      result = 31 * result + (this.internalClassNames == null ? 0 : this.internalClassNames.hashCode());
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
      StringOrClassMatcher other = (StringOrClassMatcher)obj;
      if (this.internalClassNames == null) {
        if (other.internalClassNames != null)
          return false;
      }
      else if (!this.internalClassNames.equals(other.internalClassNames)) {
        return false;
      }
      return true;
    }

    public boolean isExactClassMatcher()
    {
      return true;
    }

    public Collection<String> getClassNames()
    {
      return this.internalClassNames;
    }
  }
}