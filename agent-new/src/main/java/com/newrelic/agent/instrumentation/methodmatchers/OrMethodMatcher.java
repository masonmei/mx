//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.methodmatchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.newrelic.deps.org.objectweb.asm.commons.Method;

import com.newrelic.deps.com.google.common.collect.Lists;

public final class OrMethodMatcher extends ManyMethodMatcher {
  private OrMethodMatcher(MethodMatcher... methodMatchers) {
    super(methodMatchers);
  }

  private OrMethodMatcher(Collection<MethodMatcher> methodMatchers) {
    super(methodMatchers);
  }

  public static final MethodMatcher getMethodMatcher(MethodMatcher... matchers) {
    return getMethodMatcher((Collection) Arrays.asList(matchers));
  }

  public static final MethodMatcher getMethodMatcher(Collection<MethodMatcher> matchers) {
    if (matchers.size() == 1) {
      return (MethodMatcher) matchers.iterator().next();
    } else {
      HashMap exactMatchers = new HashMap();
      LinkedList otherMatchers = new LinkedList();
      Iterator matcher = matchers.iterator();

      while (matcher.hasNext()) {
        MethodMatcher matcher1 = (MethodMatcher) matcher.next();
        if (matcher1 instanceof ExactMethodMatcher) {
          ExactMethodMatcher m = (ExactMethodMatcher) matcher1;
          if (m.getDescriptions().isEmpty()) {
            otherMatchers.add(matcher1);
          } else {
            OrMethodMatcher.DescMethodMatcher descMatcher =
                    (OrMethodMatcher.DescMethodMatcher) exactMatchers.get(m.getName());
            if (descMatcher == null) {
              descMatcher = new OrMethodMatcher.DescMethodMatcher(m.getDescriptions());
              exactMatchers.put(m.getName().intern(), descMatcher);
            } else {
              descMatcher.addDescriptions(m.getDescriptions());
            }
          }
        } else {
          otherMatchers.add(matcher1);
        }
      }

      OrMethodMatcher.OrExactMethodMatchers matcher2 = new OrMethodMatcher.OrExactMethodMatchers(exactMatchers);
      if (otherMatchers.size() == 0) {
        return matcher2;
      } else {
        otherMatchers.add(matcher2);
        return new OrMethodMatcher(otherMatchers);
      }
    }
  }

  public boolean matches(int access, String name, String desc, Set<String> annotations) {
    Iterator i$ = this.methodMatchers.iterator();

    MethodMatcher matcher;
    do {
      if (!i$.hasNext()) {
        return false;
      }

      matcher = (MethodMatcher) i$.next();
    } while (!matcher.matches(access, name, desc, annotations));

    return true;
  }

  public String toString() {
    return "Or Match " + this.methodMatchers;
  }

  private static class DescMethodMatcher implements MethodMatcher {
    private Set<String> descriptions;

    public DescMethodMatcher(Set<String> set) {
      this.descriptions = new HashSet(set);
    }

    public void addDescriptions(Set<String> desc) {
      this.descriptions.addAll(desc);
    }

    public boolean matches(int access, String name, String desc, Set<String> annotations) {
      return this.descriptions.contains(desc);
    }

    public String toString() {
      return this.descriptions.toString();
    }

    public int hashCode() {
      boolean prime = true;
      byte result = 1;
      int result1 = 31 * result + (this.descriptions == null ? 0 : this.descriptions.hashCode());
      return result1;
    }

    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if (obj == null) {
        return false;
      } else if (this.getClass() != obj.getClass()) {
        return false;
      } else {
        OrMethodMatcher.DescMethodMatcher other = (OrMethodMatcher.DescMethodMatcher) obj;
        if (this.descriptions == null) {
          if (other.descriptions != null) {
            return false;
          }
        } else if (!this.descriptions.equals(other.descriptions)) {
          return false;
        }

        return true;
      }
    }

    public Method[] getExactMethods() {
      return null;
    }
  }

  private static class OrExactMethodMatchers implements MethodMatcher {
    private final Map<String, OrMethodMatcher.DescMethodMatcher> exactMatchers;

    public OrExactMethodMatchers(Map<String, OrMethodMatcher.DescMethodMatcher> exactMatchers) {
      this.exactMatchers = exactMatchers;
    }

    public boolean matches(int access, String name, String desc, Set<String> annotations) {
      OrMethodMatcher.DescMethodMatcher matcher =
              (OrMethodMatcher.DescMethodMatcher) this.exactMatchers.get(name);
      return matcher == null ? false : matcher.matches(access, name, desc, annotations);
    }

    public String toString() {
      return this.exactMatchers.toString();
    }

    public int hashCode() {
      boolean prime = true;
      byte result = 1;
      int result1 = 31 * result + (this.exactMatchers == null ? 0 : this.exactMatchers.hashCode());
      return result1;
    }

    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if (obj == null) {
        return false;
      } else if (this.getClass() != obj.getClass()) {
        return false;
      } else {
        OrMethodMatcher.OrExactMethodMatchers other = (OrMethodMatcher.OrExactMethodMatchers) obj;
        if (this.exactMatchers == null) {
          if (other.exactMatchers != null) {
            return false;
          }
        } else if (!this.exactMatchers.equals(other.exactMatchers)) {
          return false;
        }

        return true;
      }
    }

    public Method[] getExactMethods() {
      ArrayList methods = Lists.newArrayList();
      Iterator i$ = this.exactMatchers.entrySet().iterator();

      while (i$.hasNext()) {
        Entry entry = (Entry) i$.next();
        Iterator i$1 = ((OrMethodMatcher.DescMethodMatcher) entry.getValue()).descriptions.iterator();

        while (i$1.hasNext()) {
          String desc = (String) i$1.next();
          methods.add(new Method((String) entry.getKey(), desc));
        }
      }

      return (Method[]) methods.toArray(new Method[methods.size()]);
    }
  }
}
