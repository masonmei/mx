package com.newrelic.agent.instrumentation.methodmatchers;

import java.util.Set;

import org.objectweb.asm.commons.Method;

public class AccessMethodMatcher implements MethodMatcher {
    private final int accessFlags;

    public AccessMethodMatcher(int accessFlags) {
        this.accessFlags = accessFlags;
    }

    public boolean matches(int access, String name, String desc, Set<String> annotations) {
        return (access == -1) || ((access & accessFlags) == accessFlags);
    }

    public Method[] getExactMethods() {
        return null;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + accessFlags;
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
        AccessMethodMatcher other = (AccessMethodMatcher) obj;
        return accessFlags == other.accessFlags;
    }
}