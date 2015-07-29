package com.newrelic.agent.instrumentation.methodmatchers;

import java.util.Set;

import com.newrelic.deps.org.objectweb.asm.commons.Method;

public final class NotMethodMatcher implements MethodMatcher {
    private MethodMatcher methodMatcher;

    public NotMethodMatcher(MethodMatcher methodMatcher) {
        this.methodMatcher = methodMatcher;
    }

    public boolean matches(int access, String name, String desc, Set<String> annotations) {
        return !methodMatcher.matches(access, name, desc, annotations);
    }

    public Method[] getExactMethods() {
        return null;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
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
        NotMethodMatcher other = (NotMethodMatcher) obj;
        if (methodMatcher == null) {
            if (other.methodMatcher != null) {
                return false;
            }
        } else if (!methodMatcher.equals(other.methodMatcher)) {
            return false;
        }
        return true;
    }
}