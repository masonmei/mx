package com.newrelic.agent.instrumentation.pointcuts.container.glassfish;

import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.jmx.values.GlassfishJmxValues;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;

@com.newrelic.agent.instrumentation.pointcuts.PointCut
public class Glassfish3StartUpPointCut extends com.newrelic.agent.instrumentation.PointCut implements
        EntryInvocationHandler {
    public static final String GLASSFISH_INSTRUMENTATION_GROUP_NAME = "glassfish_instrumentation";
    private boolean addedJmx = false;

    public Glassfish3StartUpPointCut(ClassTransformer classTransformer) {
        super(new PointCutConfiguration(Glassfish3StartUpPointCut.class.getName(), "glassfish_instrumentation", true),
                     createClassMatcher(), createMethodMatcher());
    }

    private static ClassMatcher createClassMatcher() {
        return new ExactClassMatcher("com/sun/enterprise/v3/server/SystemTasks");
    }

    private static MethodMatcher createMethodMatcher() {
        return createExactMethodMatcher("postConstruct", new String[] {"()V"});
    }

    public void handleInvocation(ClassMethodSignature sig, Object object, Object[] args) {
        if (!addedJmx) {
            ServiceFactory.getJmxService().addJmxFrameworkValues(new GlassfishJmxValues());
            addedJmx = true;
            if (Agent.LOG.isFinerEnabled()) {
                Agent.LOG.log(Level.FINER, "Added JMX for Glassfish");
            }
        }
    }

    protected PointCutInvocationHandler getPointCutInvocationHandlerImpl() {
        return this;
    }
}