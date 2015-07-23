package com.newrelic.agent.instrumentation.custom;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;

public class ClassRetransformer {
    private final InstrumentationContextManager contextManager;
    private final Set<ClassMatchVisitorFactory> matchers;
    private CustomClassTransformer transformer;

    public ClassRetransformer(InstrumentationContextManager contextManager) {
        this.contextManager = contextManager;
        matchers = Sets.newHashSet();
    }

    public synchronized void setClassMethodMatchers(List<ExtensionClassAndMethodMatcher> newMatchers) {
        matchers.clear();
        if (transformer != null) {
            matchers.add(transformer.getMatcher());
            transformer.destroy();
        }

        if (newMatchers.isEmpty()) {
            transformer = null;
        } else {
            transformer = new CustomClassTransformer(contextManager, newMatchers);
            matchers.add(transformer.getMatcher());
        }
    }

    public synchronized void appendClassMethodMatchers(List<ExtensionClassAndMethodMatcher> toAdd) {
        if (transformer != null) {
            toAdd.addAll(transformer.extensionPointCuts);
        }
        setClassMethodMatchers(toAdd);
    }

    public Set<ClassMatchVisitorFactory> getMatchers() {
        return matchers;
    }
}