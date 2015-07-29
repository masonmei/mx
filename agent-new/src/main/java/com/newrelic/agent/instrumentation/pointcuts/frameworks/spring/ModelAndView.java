package com.newrelic.agent.instrumentation.pointcuts.frameworks.spring;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName={"org/springframework/web/portlet/ModelAndView", "org/springframework/web/servlet/ModelAndView"})
public abstract interface ModelAndView
{
  public abstract String getViewName();
}