package com.newrelic.agent.instrumentation.custom;

import java.util.List;
import java.util.Set;

import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.deps.com.google.common.collect.Sets;

public class ClassRetransformer {
    private final InstrumentationContextManager contextManager;
    private final Set<ClassMatchVisitorFactory> matchers;
    private CustomClassTransformer transformer;

    public ClassRetransformer(InstrumentationContextManager contextManager) {
        this.contextManager = contextManager;
        this.matchers = Sets.newHashSet();
    }

    public synchronized void setClassMethodMatchers(List<ExtensionClassAndMethodMatcher> newMatchers) {
        this.matchers.clear();
        if (this.transformer != null) {
            this.matchers.add(this.transformer.getMatcher());
            this.transformer.destroy();
        }

        if (newMatchers.isEmpty()) {
            this.transformer = null;
        } else {
            this.transformer = new CustomClassTransformer(this.contextManager, newMatchers);
            this.matchers.add(this.transformer.getMatcher());
        }
    }

    public synchronized void appendClassMethodMatchers(List<ExtensionClassAndMethodMatcher> toAdd) {
        if (this.transformer != null) {
            toAdd.addAll(this.transformer.extensionPointCuts);
        }
        setClassMethodMatchers(toAdd);
    }

    public Set<ClassMatchVisitorFactory> getMatchers() {
        return this.matchers;
    }
}