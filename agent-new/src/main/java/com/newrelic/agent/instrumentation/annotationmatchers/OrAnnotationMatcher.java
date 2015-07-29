package com.newrelic.agent.instrumentation.annotationmatchers;

import java.util.Arrays;
import java.util.Collection;

public class OrAnnotationMatcher
  implements AnnotationMatcher
{
  private final Collection<AnnotationMatcher> matchers;

  private OrAnnotationMatcher(Collection<AnnotationMatcher> matchers)
  {
    this.matchers = matchers;
  }

  public boolean matches(String annotationDesc)
  {
    for (AnnotationMatcher matcher : this.matchers) {
      if (matcher.matches(annotationDesc)) {
        return true;
      }
    }
    return false;
  }

  public static AnnotationMatcher getOrMatcher(AnnotationMatcher[] matchers) {
    if (matchers.length == 1) {
      return matchers[0];
    }
    return new OrAnnotationMatcher(Arrays.asList(matchers));
  }
}