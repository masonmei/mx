package com.newrelic.agent.service.module;

import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.service.Service;

public abstract interface JarCollectorService extends Service
{
  public abstract ClassMatchVisitorFactory getSourceVisitor();
}