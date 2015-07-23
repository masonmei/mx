package com.newrelic.agent.instrumentation.classmatchers;

import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;

public class HashSafeClassAndMethodMatcher extends DefaultClassAndMethodMatcher {
    public HashSafeClassAndMethodMatcher(ClassMatcher classMatcher, MethodMatcher methodMatcher) {
        super(classMatcher, methodMatcher);
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (classMatcher == null ? 0 : classMatcher.hashCode());
        result = 31 * result + (methodMatcher == null ? 0 : methodMatcher.hashCode());
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
        DefaultClassAndMethodMatcher other = (DefaultClassAndMethodMatcher) obj;
        if (classMatcher == null) {
            if (other.getClassMatcher() != null) {
                return false;
            }
        } else if (!classMatcher.equals(other.getClassMatcher())) {
            return false;
        }
        if (methodMatcher == null) {
            if (other.getMethodMatcher() != null) {
                return false;
            }
        } else if (!methodMatcher.equals(other.getMethodMatcher())) {
            return false;
        }
        return true;
    }
}