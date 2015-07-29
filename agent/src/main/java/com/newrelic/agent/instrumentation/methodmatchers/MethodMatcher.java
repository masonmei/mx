package com.newrelic.agent.instrumentation.methodmatchers;

import java.util.Set;

import com.newrelic.deps.com.google.common.collect.ImmutableSet;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

public interface MethodMatcher {
    Set<String> UNSPECIFIED_ANNOTATIONS = ImmutableSet.of();
    int UNSPECIFIED_ACCESS = -1;

    boolean matches(int paramInt, String paramString1, String paramString2, Set<String> paramSet);

    boolean equals(Object paramObject);

    Method[] getExactMethods();
}