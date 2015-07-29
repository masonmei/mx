package com.newrelic.agent.profile.method;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.newrelic.agent.instrumentation.InstrumentedMethod;
import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.com.google.common.collect.Maps;

public class MultipleMethodInfo extends MethodInfo {
    private final Set<Member> possibleMethods;

    public MultipleMethodInfo(Set<Member> methods) {
        this.possibleMethods = methods;
    }

    public List<Map<String, Object>> getJsonMethodMaps() {
        List methodList = Lists.newArrayList();

        for (Member current : this.possibleMethods) {
            Map oneMethod = Maps.newHashMap();
            addOneMethodArgs(oneMethod, MethodInfoUtil.getArguments(current));
            addOneMethodInstrumentedInfo(oneMethod, (InstrumentedMethod) ((AnnotatedElement) current)
                                                                                 .getAnnotation(InstrumentedMethod.class));

            methodList.add(oneMethod);
        }

        return methodList;
    }
}