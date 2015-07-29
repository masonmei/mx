package com.newrelic.agent.instrumentation;

import java.lang.instrument.Instrumentation;
import java.util.Collection;

import com.newrelic.agent.instrumentation.classmatchers.ClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.agent.instrumentation.custom.ClassRetransformer;
import com.newrelic.agent.service.Service;

public abstract interface ClassTransformerService extends Service {
    public abstract ClassTransformer getClassTransformer();

    public abstract ClassRetransformer getLocalRetransformer();

    public abstract ClassRetransformer getRemoteRetransformer();

    public abstract void checkShutdown();

    public abstract InstrumentationContextManager getContextManager();

    public abstract boolean addTraceMatcher(ClassAndMethodMatcher paramClassAndMethodMatcher, String paramString);

    public abstract void retransformMatchingClasses(Collection<ClassMatchVisitorFactory> paramCollection);

    public abstract void retransformMatchingClassesImmediately(Collection<ClassMatchVisitorFactory> paramCollection);

    public abstract Instrumentation getExtensionInstrumentation();
}