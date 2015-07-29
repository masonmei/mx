package com.newrelic.agent.instrumentation.methodmatchers;

import java.util.Set;

public final class AndMethodMatcher extends ManyMethodMatcher {
    protected AndMethodMatcher(MethodMatcher[] methodMatchers) {
        super(methodMatchers);
    }

    public static final MethodMatcher getMethodMatcher(MethodMatcher[] matchers) {
        if (matchers.length == 1) {
            return matchers[0];
        }
        return new AndMethodMatcher(matchers);
    }

    public boolean matches(int access, String name, String desc, Set<String> annotations) {
        for (MethodMatcher matcher : this.methodMatchers) {
            if (!matcher.matches(access, name, desc, annotations)) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return "And Match " + this.methodMatchers;
    }
}