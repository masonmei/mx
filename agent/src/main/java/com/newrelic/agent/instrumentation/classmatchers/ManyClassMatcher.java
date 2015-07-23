package com.newrelic.agent.instrumentation.classmatchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public abstract class ManyClassMatcher extends ClassMatcher {
    private final ClassMatcher[] matchers;
    private final boolean isExact;

    public ManyClassMatcher(Collection<ClassMatcher> matchers) {
        this.matchers = ((ClassMatcher[]) matchers.toArray(new ClassMatcher[matchers.size()]));
        isExact = determineIfExact(this.matchers);
    }

    private static boolean determineIfExact(ClassMatcher[] matchers) {
        for (ClassMatcher matcher : matchers) {
            if (!matcher.isExactClassMatcher()) {
                return false;
            }
        }
        return true;
    }

    public boolean isExactClassMatcher() {
        return isExact;
    }

    protected ClassMatcher[] getClassMatchers() {
        return matchers;
    }

    public Collection<String> getClassNames() {
        Collection classNames = new ArrayList();
        for (ClassMatcher matcher : matchers) {
            classNames.addAll(matcher.getClassNames());
        }
        return classNames;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + Arrays.hashCode(matchers);
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
        ManyClassMatcher other = (ManyClassMatcher) obj;
        if (!Arrays.equals(matchers, other.matchers)) {
            return false;
        }
        return true;
    }

    public String toString() {
        return getClass().getSimpleName() + "(" + matchers + ")";
    }
}