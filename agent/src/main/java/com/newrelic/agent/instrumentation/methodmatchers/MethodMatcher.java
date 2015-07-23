package com.newrelic.agent.instrumentation.methodmatchers;

import java.util.Set;

import org.objectweb.asm.commons.Method;

import com.google.common.collect.ImmutableSet;

public abstract interface MethodMatcher {
    public static final Set<String> UNSPECIFIED_ANNOTATIONS = ImmutableSet.of();
    public static final int UNSPECIFIED_ACCESS = -1;

    public abstract boolean matches(int paramInt, String paramString1, String paramString2, Set<String> paramSet);

    public abstract boolean equals(Object paramObject);

    public abstract Method[] getExactMethods();
}