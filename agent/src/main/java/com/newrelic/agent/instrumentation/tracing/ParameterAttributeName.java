package com.newrelic.agent.instrumentation.tracing;

import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;

public class ParameterAttributeName {
    private final String attributeName;
    private final int index;
    private final MethodMatcher methodMatcher;

    public ParameterAttributeName(int index, String attributeName, MethodMatcher methodMatcher) {
        this.index = index;
        this.attributeName = attributeName;
        this.methodMatcher = methodMatcher;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public int getIndex() {
        return index;
    }

    public MethodMatcher getMethodMatcher() {
        return methodMatcher;
    }

    public String toString() {
        return "ParameterAttributeName [attributeName=" + attributeName + ", index=" + index + ", methodMatcher="
                       + methodMatcher + "]";
    }
}