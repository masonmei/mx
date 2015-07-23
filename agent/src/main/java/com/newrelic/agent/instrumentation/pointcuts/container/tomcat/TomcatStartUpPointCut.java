package com.newrelic.agent.instrumentation.pointcuts.container.tomcat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OrClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.jmx.values.TomcatJmxValues;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;

@com.newrelic.agent.instrumentation.pointcuts.PointCut
public class TomcatStartUpPointCut extends com.newrelic.agent.instrumentation.PointCut implements
        EntryInvocationHandler {
    static final String TOMCAT_INSTRUMENTATION_GROUP_NAME = "tomcat";
    private final AtomicBoolean addedJmx = new AtomicBoolean(false);

    public TomcatStartUpPointCut(ClassTransformer classTransformer) {
        super(new PointCutConfiguration(TomcatStartUpPointCut.class.getName(), "tomcat", true), createClassMatcher(),
                     OrMethodMatcher.getMethodMatcher(new MethodMatcher[] {createExactMethodMatcher("start",
                                                                                                           new String[] {"()V"}),
                                                                                  createExactMethodMatcher("getServer",
                                                                                                                  new String[] {"()Lorg/apache/catalina/Server;"})}));
    }

    private static ClassMatcher createClassMatcher() {
        return new OrClassMatcher(new ClassMatcher[] {new ExactClassMatcher("org/apache/catalina/startup/HostConfig"),
                                                             new ExactClassMatcher
                                                                     ("org/apache/catalina/startup/Embedded"),
                                                             new ExactClassMatcher
                                                                     ("org/apache/catalina/startup/Tomcat")});
    }

    public void handleInvocation(ClassMethodSignature sig, Object object, Object[] args) {
        if ((System.getProperty("com.sun.aas.installRoot") == null) && (!addedJmx.getAndSet(true))) {
            ServiceFactory.getJmxService().addJmxFrameworkValues(new TomcatJmxValues());
            if (Agent.LOG.isFinerEnabled()) {
                Agent.LOG.log(Level.FINER, "Added JMX for Tomcat");
            }
        }
    }

    protected PointCutInvocationHandler getPointCutInvocationHandlerImpl() {
        return this;
    }
}