package com.newrelic.agent.instrumentation;

import java.lang.instrument.Instrumentation;
import java.util.Collection;

import com.newrelic.agent.instrumentation.classmatchers.ClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.agent.instrumentation.custom.ClassRetransformer;
import com.newrelic.agent.service.Service;

public interface ClassTransformerService extends Service {
    ClassTransformer getClassTransformer();

    ClassRetransformer getLocalRetransformer();

    ClassRetransformer getRemoteRetransformer();

    void checkShutdown();

    InstrumentationContextManager getContextManager();

    boolean addTraceMatcher(ClassAndMethodMatcher paramClassAndMethodMatcher, String paramString);

    void retransformMatchingClasses(Collection<ClassMatchVisitorFactory> paramCollection);

    void retransformMatchingClassesImmediately(Collection<ClassMatchVisitorFactory> paramCollection);

    Instrumentation getExtensionInstrumentation();
}