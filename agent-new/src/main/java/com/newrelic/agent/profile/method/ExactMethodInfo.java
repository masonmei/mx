package com.newrelic.agent.profile.method;

import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.agent.instrumentation.InstrumentedMethod;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.List;
import java.util.Map;

public class ExactMethodInfo extends MethodInfo
{
  private final List<String> arguments;
  private final InstrumentedMethod annotation;

  public ExactMethodInfo(List<String> pArguments, Member method)
  {
    this.arguments = pArguments;
    this.annotation = ((InstrumentedMethod)((AnnotatedElement)method).getAnnotation(InstrumentedMethod.class));
  }

  public List<Map<String, Object>> getJsonMethodMaps()
  {
    List methodList = Lists.newArrayList();

    Map oneMethod = Maps.newHashMap();
    addOneMethodArgs(oneMethod, this.arguments);
    addOneMethodInstrumentedInfo(oneMethod, this.annotation);

    methodList.add(oneMethod);
    return methodList;
  }
}