package com.newrelic.agent.tracers;

import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;

public final class ClassMethodSignature {
    private final String className;
    private final String methodName;
    private final String methodDesc;
    private ClassMethodMetricNameFormat customMetricName;
    private ClassMethodMetricNameFormat javaMetricName;

    public ClassMethodSignature(String className, String methodName, String methodDesc) {
        this.className = className;
        this.methodName = methodName;
        this.methodDesc = methodDesc;

        customMetricName = new ClassMethodMetricNameFormat(this, null, "Custom");
        javaMetricName = new ClassMethodMetricNameFormat(this, null, "Java");
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    public String toString() {
        return className + '.' + methodName + methodDesc;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (className == null ? 0 : className.hashCode());
        result = 31 * result + (methodDesc == null ? 0 : methodDesc.hashCode());
        result = 31 * result + (methodName == null ? 0 : methodName.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ClassMethodSignature other = (ClassMethodSignature) obj;
        if (className == null) {
            if (other.className != null) {
                return false;
            }
        } else if (!className.equals(other.className)) {
            return false;
        }
        if (methodDesc == null) {
            if (other.methodDesc != null) {
                return false;
            }
        } else if (!methodDesc.equals(other.methodDesc)) {
            return false;
        }
        if (methodName == null) {
            if (other.methodName != null) {
                return false;
            }
        } else if (!methodName.equals(other.methodName)) {
            return false;
        }
        return true;
    }

    public ClassMethodSignature intern() {
        return new ClassMethodSignature(className.intern(), methodName.intern(), methodDesc.intern());
    }

    public MetricNameFormat getMetricNameFormat(Object invocationTarget, int flags) {
        if ((invocationTarget == null) || (className.equals(invocationTarget.getClass().getName()))) {
            return TracerFlags.isCustom(flags) ? customMetricName : javaMetricName;
        }
        return new ClassMethodMetricNameFormat(this, invocationTarget, TracerFlags.isCustom(flags) ? "Custom" : "Java");
    }
}