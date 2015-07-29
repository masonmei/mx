package com.newrelic.agent.instrumentation.methodmatchers;

import com.newrelic.agent.Agent;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.Method;
import com.newrelic.agent.logging.IAgentLogger;
import java.util.Set;

public class AnnotationMethodMatcher
  implements MethodMatcher
{
  private final Type annotationType;
  private final String annotationDesc;

  public AnnotationMethodMatcher(Type annotationType)
  {
    this.annotationType = annotationType;
    this.annotationDesc = annotationType.getDescriptor();
  }

  public boolean matches(int access, String name, String desc, Set<String> annotations)
  {
    if (annotations == MethodMatcher.UNSPECIFIED_ANNOTATIONS) {
      Agent.LOG.finer("The annotation method matcher will not work if annotations aren't specified");
    }
    return annotations.contains(this.annotationDesc);
  }

  public Method[] getExactMethods()
  {
    return null;
  }

  public Type getAnnotationType() {
    return this.annotationType;
  }

  public int hashCode()
  {
    int prime = 31;
    int result = 1;
    result = 31 * result + (this.annotationDesc == null ? 0 : this.annotationDesc.hashCode());
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
    AnnotationMethodMatcher other = (AnnotationMethodMatcher)obj;
    if (this.annotationType == null) {
      if (other.annotationType != null)
        return false;
    } else if (!this.annotationType.equals(other.annotationType))
      return false;
    return true;
  }
}