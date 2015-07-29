package com.newrelic.agent.instrumentation.pointcuts.container.weblogic;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.jmx.JmxService;
import com.newrelic.agent.jmx.values.WeblogicJmxValues;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import java.util.logging.Level;

@com.newrelic.agent.instrumentation.pointcuts.PointCut
public class WeblogicStartUpPointCut extends com.newrelic.agent.instrumentation.PointCut
  implements EntryInvocationHandler
{
  private boolean addedJmx = false;

  public WeblogicStartUpPointCut(ClassTransformer classTransformer) {
    super(new PointCutConfiguration(WeblogicStartUpPointCut.class.getName(), null, true), createClassMatcher(), createExactMethodMatcher("run", new String[] { "([Ljava/lang/String;)I" }));
  }

  private static ClassMatcher createClassMatcher()
  {
    return new ExactClassMatcher("weblogic/t3/srvr/T3Srvr");
  }

  public void handleInvocation(ClassMethodSignature sig, Object object, Object[] args)
  {
    if (!this.addedJmx) {
      ServiceFactory.getJmxService().addJmxFrameworkValues(new WeblogicJmxValues());
      this.addedJmx = true;
      if (Agent.LOG.isFinerEnabled())
        Agent.LOG.log(Level.FINER, "Added JMX for Weblogic");
    }
  }

  protected PointCutInvocationHandler getPointCutInvocationHandlerImpl()
  {
    return this;
  }
}