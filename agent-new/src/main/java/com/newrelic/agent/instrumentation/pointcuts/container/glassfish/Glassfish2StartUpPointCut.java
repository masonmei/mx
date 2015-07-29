package com.newrelic.agent.instrumentation.pointcuts.container.glassfish;

import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.jmx.values.Glassfish2JmxValues;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;

@com.newrelic.agent.instrumentation.pointcuts.PointCut
public class Glassfish2StartUpPointCut extends com.newrelic.agent.instrumentation.PointCut implements
        EntryInvocationHandler {
    private boolean addedJmx = false;

    public Glassfish2StartUpPointCut(ClassTransformer classTransformer) {
        super(new PointCutConfiguration(Glassfish2StartUpPointCut.class.getName(), "glassfish_instrumentation", true),
                     createClassMatcher(), createMethodMatcher());
    }

    private static ClassMatcher createClassMatcher() {
        return new ExactClassMatcher("com/sun/enterprise/server/PEMain");
    }

    private static MethodMatcher createMethodMatcher() {
        return createExactMethodMatcher("run", new String[] {"(Ljava/lang/String;)V"});
    }

    public void handleInvocation(ClassMethodSignature sig, Object object, Object[] args) {
        if (!this.addedJmx) {
            ServiceFactory.getJmxService().addJmxFrameworkValues(new Glassfish2JmxValues());
            this.addedJmx = true;
            if (Agent.LOG.isFinerEnabled()) {
                Agent.LOG.log(Level.FINER, "Added JMX for Glassfish 2");
            }
        }
    }

    protected PointCutInvocationHandler getPointCutInvocationHandlerImpl() {
        return this;
    }
}