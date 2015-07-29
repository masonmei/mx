package com.newrelic.agent.instrumentation.methodmatchers;

import java.util.Set;

import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

import com.newrelic.agent.Agent;

public class AnnotationMethodMatcher implements MethodMatcher {
    private final Type annotationType;
    private final String annotationDesc;

    public AnnotationMethodMatcher(Type annotationType) {
        this.annotationType = annotationType;
        annotationDesc = annotationType.getDescriptor();
    }

    public boolean matches(int access, String name, String desc, Set<String> annotations) {
        if (annotations == MethodMatcher.UNSPECIFIED_ANNOTATIONS) {
            Agent.LOG.finer("The annotation method matcher will not work if annotations aren't specified");
        }
        return annotations.contains(annotationDesc);
    }

    public Method[] getExactMethods() {
        return null;
    }

    public Type getAnnotationType() {
        return annotationType;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (annotationDesc == null ? 0 : annotationDesc.hashCode());
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
        AnnotationMethodMatcher other = (AnnotationMethodMatcher) obj;
        if (annotationType == null) {
            if (other.annotationType != null) {
                return false;
            }
        } else if (!annotationType.equals(other.annotationType)) {
            return false;
        }
        return true;
    }
}