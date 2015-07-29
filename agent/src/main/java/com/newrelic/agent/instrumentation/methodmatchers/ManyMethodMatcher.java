package com.newrelic.agent.instrumentation.methodmatchers;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

public abstract class ManyMethodMatcher implements MethodMatcher {
    protected final Collection<MethodMatcher> methodMatchers;

    protected ManyMethodMatcher(MethodMatcher[] methodMatchers) {
        this(Arrays.asList(methodMatchers));
    }

    public ManyMethodMatcher(Collection<MethodMatcher> methodMatchers) {
        this.methodMatchers = methodMatchers;
    }

    public Collection<MethodMatcher> getMethodMatchers() {
        return methodMatchers;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (methodMatchers == null ? 0 : methodMatchers.hashCode());
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
        ManyMethodMatcher other = (ManyMethodMatcher) obj;
        if (methodMatchers == null) {
            if (other.methodMatchers != null) {
                return false;
            }
        } else if ((methodMatchers.size() != other.methodMatchers.size()) || (!methodMatchers
                                                                                       .containsAll(other.methodMatchers))) {
            return false;
        }
        return true;
    }

    public Method[] getExactMethods() {
        List methods = Lists.newArrayList();
        for (MethodMatcher matcher : methodMatchers) {
            Method[] exactMethods = matcher.getExactMethods();
            if (exactMethods == null) {
                return null;
            }
            methods.addAll(Arrays.asList(exactMethods));
        }
        return (Method[]) methods.toArray(new Method[methods.size()]);
    }
}