package com.newrelic.agent.instrumentation.pointcuts.solr;

import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;

abstract class AbstractSolrPointCut extends TracerFactoryPointCut {
    static final String SOLR_CONFIG_GROUP_NAME = "solr";

    protected AbstractSolrPointCut(Class<? extends AbstractSolrPointCut> pointCutClass, ClassMatcher classMatcher,
                                   MethodMatcher methodMatcher) {
        super(new PointCutConfiguration(pointCutClass.getName(), "solr", true), classMatcher, methodMatcher);
    }

    protected AbstractSolrPointCut(PointCutConfiguration config, ClassMatcher classMatcher,
                                   MethodMatcher methodMatcher) {
        super(config, classMatcher, methodMatcher);
    }
}