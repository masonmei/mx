package com.newrelic.agent.tracers;

import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;

public final class ClassMethodSignature
{
  private final String className;
  private final String methodName;
  private final String methodDesc;
  private ClassMethodMetricNameFormat customMetricName;
  private ClassMethodMetricNameFormat javaMetricName;

  public ClassMethodSignature(String className, String methodName, String methodDesc)
  {
    this.className = className;
    this.methodName = methodName;
    this.methodDesc = methodDesc;

    this.customMetricName = new ClassMethodMetricNameFormat(this, null, "Custom");
    this.javaMetricName = new ClassMethodMetricNameFormat(this, null, "Java");
  }

  public String getClassName()
  {
    return this.className;
  }

  public String getMethodName() {
    return this.methodName;
  }

  public String getMethodDesc() {
    return this.methodDesc;
  }

  public String toString()
  {
    return this.className + '.' + this.methodName + this.methodDesc;
  }

  public int hashCode()
  {
    int prime = 31;
    int result = 1;
    result = 31 * result + (this.className == null ? 0 : this.className.hashCode());
    result = 31 * result + (this.methodDesc == null ? 0 : this.methodDesc.hashCode());
    result = 31 * result + (this.methodName == null ? 0 : this.methodName.hashCode());
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
    ClassMethodSignature other = (ClassMethodSignature)obj;
    if (this.className == null) {
      if (other.className != null)
        return false;
    }
    else if (!this.className.equals(other.className)) {
      return false;
    }
    if (this.methodDesc == null) {
      if (other.methodDesc != null)
        return false;
    }
    else if (!this.methodDesc.equals(other.methodDesc)) {
      return false;
    }
    if (this.methodName == null) {
      if (other.methodName != null)
        return false;
    }
    else if (!this.methodName.equals(other.methodName)) {
      return false;
    }
    return true;
  }

  public ClassMethodSignature intern() {
    return new ClassMethodSignature(this.className.intern(), this.methodName.intern(), this.methodDesc.intern());
  }

  public MetricNameFormat getMetricNameFormat(Object invocationTarget, int flags)
  {
    if ((invocationTarget == null) || (this.className.equals(invocationTarget.getClass().getName()))) {
      return TracerFlags.isCustom(flags) ? this.customMetricName : this.javaMetricName;
    }
    return new ClassMethodMetricNameFormat(this, invocationTarget, TracerFlags.isCustom(flags) ? "Custom" : "Java");
  }
}