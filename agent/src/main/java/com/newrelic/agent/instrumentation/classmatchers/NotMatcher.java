package com.newrelic.agent.instrumentation.classmatchers;

import java.util.Collection;
import java.util.Collections;

import org.objectweb.asm.ClassReader;

public class NotMatcher extends ClassMatcher {
    private final ClassMatcher matcher;

    public NotMatcher(ClassMatcher notMatch) {
        matcher = notMatch;
    }

    public boolean isMatch(ClassLoader loader, ClassReader cr) {
        return !matcher.isMatch(loader, cr);
    }

    public boolean isMatch(Class<?> clazz) {
        return !matcher.isMatch(clazz);
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (matcher == null ? 0 : matcher.hashCode());
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
        NotMatcher other = (NotMatcher) obj;
        if (matcher == null) {
            if (other.matcher != null) {
                return false;
            }
        } else if (!matcher.equals(other.matcher)) {
            return false;
        }
        return true;
    }

    public Collection<String> getClassNames() {
        return Collections.emptyList();
    }
}