package com.newrelic.agent.instrumentation.pointcuts.frameworks;

import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;

public abstract class AbstractPortletPointCut extends TracerFactoryPointCut {
    public AbstractPortletPointCut(Class<? extends TracerFactoryPointCut> tracerFactory, MethodMatcher methodMatcher) {
        super(tracerFactory, new InterfaceMatcher("javax/portlet/Portlet"), methodMatcher);
    }
}