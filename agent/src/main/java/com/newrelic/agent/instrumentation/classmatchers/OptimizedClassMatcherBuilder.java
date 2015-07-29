package com.newrelic.agent.instrumentation.classmatchers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import com.newrelic.deps.org.objectweb.asm.commons.Method;

import com.newrelic.deps.com.google.common.base.Supplier;
import com.newrelic.deps.com.google.common.collect.Multimaps;
import com.newrelic.deps.com.google.common.collect.SetMultimap;
import com.newrelic.deps.com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.methodmatchers.AnnotationMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;

public class OptimizedClassMatcherBuilder {
    private static Supplier<Set<ClassAndMethodMatcher>> CLASS_AND_METHOD_MATCHER_SET_SUPPLIER = new Supplier() {
        public Set<ClassAndMethodMatcher> get() {
            return Sets.newSetFromMap(new HashMap());
        }
    };

    private final SetMultimap<MethodMatcher, ClassAndMethodMatcher> methodMatchers =
            Multimaps.newSetMultimap(new HashMap(), CLASS_AND_METHOD_MATCHER_SET_SUPPLIER);

    private final SetMultimap<Method, ClassAndMethodMatcher> methods =
            Multimaps.newSetMultimap(new HashMap(), CLASS_AND_METHOD_MATCHER_SET_SUPPLIER);

    private final Set<String> methodAnnotationMatchers = Sets.newHashSet();

    private final Set<String> exactClassNames = Sets.newHashSet();
    private boolean exactClassMatch = true;

    public static OptimizedClassMatcherBuilder newBuilder() {
        return new OptimizedClassMatcherBuilder();
    }

    public OptimizedClassMatcherBuilder addClassMethodMatcher(ClassAndMethodMatcher[] matchers) {
        for (ClassAndMethodMatcher matcher : matchers) {
            if ((exactClassMatch) && (!matcher.getClassMatcher().isExactClassMatcher())) {
                exactClassMatch = false;
            } else {
                exactClassNames.addAll(matcher.getClassMatcher().getClassNames());
            }
            if ((matcher.getMethodMatcher() instanceof AnnotationMethodMatcher)) {
                methodAnnotationMatchers.add(((AnnotationMethodMatcher) matcher.getMethodMatcher()).getAnnotationType()
                                                     .getDescriptor());
            }
            Method[] exactMethods = matcher.getMethodMatcher().getExactMethods();
            if ((exactMethods == null) || (exactMethods.length == 0)) {
                methodMatchers.put(matcher.getMethodMatcher(), matcher);
            } else {
                for (Method m : exactMethods) {
                    if (OptimizedClassMatcher.METHODS_WE_NEVER_INSTRUMENT.contains(m)) {
                        Agent.LOG.severe("Skipping method matcher for method " + m);
                        Agent.LOG.fine("Skipping matcher for class matcher " + matcher.getClassMatcher());
                    } else {
                        if (OptimizedClassMatcher.DEFAULT_CONSTRUCTOR.equals(m)) {
                            Agent.LOG.severe("Instrumentation is matching a default constructor.  This may result in "
                                                     + "slow class loading times.");
                            Agent.LOG.debug("No arg constructor matcher: " + matcher.getClassMatcher());
                        }
                        methods.put(m, matcher);
                    }
                }
            }
        }
        return this;
    }

    public OptimizedClassMatcherBuilder copyFrom(ClassMatchVisitorFactory otherMatcher) {
        if ((otherMatcher instanceof OptimizedClassMatcher)) {
            OptimizedClassMatcher matcher = (OptimizedClassMatcher) otherMatcher;
            methodAnnotationMatchers.addAll(matcher.methodAnnotationsToMatch);
            for (Entry<MethodMatcher, ClassAndMethodMatcher> entry : matcher.methodMatchers) {
                methodMatchers.put(entry.getKey(), entry.getValue());
            }
            for (Entry<Method, Collection<ClassAndMethodMatcher>> entry : matcher.methods.entrySet()) {
                methods.putAll(entry.getKey(), (Iterable) entry.getValue());
            }
            if (null != matcher.exactClassNames) {
                exactClassNames.addAll(matcher.exactClassNames);
            }
        } else {
            throw new UnsupportedOperationException("Unable to copy unexpected type " + otherMatcher.getClass()
                                                                                                .getName());
        }

        return this;
    }

    public ClassMatchVisitorFactory build() {
        if ((methodMatchers.isEmpty()) && (methods.isEmpty()) && (methodAnnotationMatchers.isEmpty())) {
            Agent.LOG.finest("Creating an empty class/method matcher");
            return OptimizedClassMatcher.EMPTY_MATCHER;
        }
        Set exactClassNames = null;
        if (exactClassMatch) {
            exactClassNames = this.exactClassNames;
        }
        return new OptimizedClassMatcher(methodAnnotationMatchers, methods, methodMatchers, exactClassNames);
    }
}