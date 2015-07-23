package com.newrelic.agent.instrumentation.annotationmatchers;

public class ClassNameAnnotationMatcher implements AnnotationMatcher {
    private final String simpleClassName;
    private final boolean fullMatch;

    public ClassNameAnnotationMatcher(String className) {
        this(className, true);
    }

    public ClassNameAnnotationMatcher(String className, boolean fullMatch) {
        if ((!fullMatch) && (!className.endsWith(";"))) {
            className = className + ";";
        }
        simpleClassName = className;
        this.fullMatch = fullMatch;
    }

    public boolean matches(String annotationDesc) {
        if (fullMatch) {
            return annotationDesc.equals(simpleClassName);
        }
        return annotationDesc.endsWith(simpleClassName);
    }
}