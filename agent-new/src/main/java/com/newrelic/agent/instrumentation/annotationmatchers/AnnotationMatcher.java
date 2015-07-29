package com.newrelic.agent.instrumentation.annotationmatchers;

public abstract interface AnnotationMatcher {
    public abstract boolean matches(String paramString);
}