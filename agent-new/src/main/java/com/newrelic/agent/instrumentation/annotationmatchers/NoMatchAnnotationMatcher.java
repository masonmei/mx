package com.newrelic.agent.instrumentation.annotationmatchers;

public class NoMatchAnnotationMatcher implements AnnotationMatcher {
    public boolean matches(String annotationDesc) {
        return false;
    }
}