package com.newrelic.agent.instrumentation.pointcuts.container.resin;

import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.jmx.values.ResinJmxValues;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;

@com.newrelic.agent.instrumentation.pointcuts.PointCut
public class ResinStartupPointCut extends com.newrelic.agent.instrumentation.PointCut implements
        EntryInvocationHandler {
    public static final String RESIN_INSTRUMENTATION_GROUP_NAME = "resin_instrumentation";
    private boolean addJmx = false;

    public ResinStartupPointCut(ClassTransformer classTransformer) {
        super(new PointCutConfiguration(ResinStartupPointCut.class.getName(), "resin_instrumentation", true),
                     new ExactClassMatcher("com.caucho.server.resin/Resin"),
                     createExactMethodMatcher("start", new String[] {"()V"}));
    }

    public void handleInvocation(ClassMethodSignature sig, Object object, Object[] args) {
        if (!addJmx) {
            ServiceFactory.getJmxService().addJmxFrameworkValues(new ResinJmxValues());
            addJmx = true;
            if (Agent.LOG.isFinerEnabled()) {
                Agent.LOG.log(Level.FINER, "Added JMX for Resin");
            }
        }
    }

    protected PointCutInvocationHandler getPointCutInvocationHandlerImpl() {
        return this;
    }
}