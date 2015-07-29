package com.newrelic.agent.instrumentation.pointcuts.container.jboss;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.jmx.JmxService;
import com.newrelic.agent.jmx.values.Jboss7UpJmxValues;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import java.util.logging.Level;

@com.newrelic.agent.instrumentation.pointcuts.PointCut
public class Jboss7StartupPointCut extends com.newrelic.agent.instrumentation.PointCut
  implements EntryInvocationHandler
{
  public static final String JBOSS_INSTRUMENTATION_GROUP_NAME = "jboss_instrumentation";
  private boolean addedJmx = false;

  public Jboss7StartupPointCut(ClassTransformer classTransformer) {
    super(new PointCutConfiguration(Jboss7StartupPointCut.class.getName(), "jboss_instrumentation", true), createClassMatcher(), new ExactMethodMatcher("installMBeanServer", "()V"));
  }

  private static ClassMatcher createClassMatcher()
  {
    return new ExactClassMatcher("org/jboss/modules/ModuleLoader");
  }

  public void handleInvocation(ClassMethodSignature sig, Object object, Object[] args)
  {
    if (!this.addedJmx) {
      ServiceFactory.getJmxService().addJmxFrameworkValues(new Jboss7UpJmxValues());
      this.addedJmx = true;
      if (Agent.LOG.isFinerEnabled())
        Agent.LOG.log(Level.FINER, "Added JMX for Jboss/Wildfly");
    }
  }

  protected PointCutInvocationHandler getPointCutInvocationHandlerImpl()
  {
    return this;
  }
}