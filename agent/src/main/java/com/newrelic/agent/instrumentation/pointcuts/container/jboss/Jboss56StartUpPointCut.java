package com.newrelic.agent.instrumentation.pointcuts.container.jboss;

import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.jmx.values.Jboss56JmxValues;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;

@com.newrelic.agent.instrumentation.pointcuts.PointCut
public class Jboss56StartUpPointCut extends com.newrelic.agent.instrumentation.PointCut implements
        EntryInvocationHandler {
    private boolean addedJmx = false;

    public Jboss56StartUpPointCut(ClassTransformer classTransformer) {
        super(new PointCutConfiguration(Jboss56StartUpPointCut.class.getName(), "jboss_instrumentation", true),
                     createClassMatcher(), createExactMethodMatcher("boot", new String[] {"([Ljava/lang/String;)V"}));
    }

    private static ClassMatcher createClassMatcher() {
        return new ExactClassMatcher("org/jboss/Main");
    }

    public void handleInvocation(ClassMethodSignature sig, Object object, Object[] args) {
        if (!addedJmx) {
            ServiceFactory.getJmxService().addJmxFrameworkValues(new Jboss56JmxValues());
            addedJmx = true;
            if (Agent.LOG.isFinerEnabled()) {
                Agent.LOG.log(Level.FINER, "Added JMX for Jboss");
            }
        }
    }

    protected PointCutInvocationHandler getPointCutInvocationHandlerImpl() {
        return this;
    }
}