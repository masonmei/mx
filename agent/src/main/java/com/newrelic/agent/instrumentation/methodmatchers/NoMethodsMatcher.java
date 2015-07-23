package com.newrelic.agent.instrumentation.methodmatchers;

import java.util.Set;

import org.objectweb.asm.commons.Method;

public final class NoMethodsMatcher implements MethodMatcher {
    public boolean matches(int access, String name, String desc, Set<String> annotations) {
        return false;
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
        return true;
    }

    public int hashCode() {
        return super.hashCode();
    }

    public Method[] getExactMethods() {
        return null;
    }
}