package com.newrelic.agent.instrumentation.classmatchers;

import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;

public class DefaultClassAndMethodMatcher implements ClassAndMethodMatcher {
    protected final ClassMatcher classMatcher;
    protected final MethodMatcher methodMatcher;

    public DefaultClassAndMethodMatcher(ClassMatcher classMatcher, MethodMatcher methodMatcher) {
        this.classMatcher = classMatcher;
        this.methodMatcher = methodMatcher;
    }

    public ClassMatcher getClassMatcher() {
        return classMatcher;
    }

    public MethodMatcher getMethodMatcher() {
        return methodMatcher;
    }
}