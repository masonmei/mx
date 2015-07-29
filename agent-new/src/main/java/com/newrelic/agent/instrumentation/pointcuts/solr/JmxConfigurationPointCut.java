package com.newrelic.agent.instrumentation.pointcuts.solr;

import java.lang.reflect.Field;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.MethodExitTracer;
import com.newrelic.agent.tracers.Tracer;

@PointCut
public class JmxConfigurationPointCut extends AbstractSolrPointCut {
    public JmxConfigurationPointCut(ClassTransformer classTransformer) {
        super(new PointCutConfiguration(JmxConfigurationPointCut.class.getName(), "solr",
                                               ServiceFactory.getJmxService().isEnabled()),
                     new ExactClassMatcher("org/apache/solr/core/SolrConfig$JmxConfiguration"),
                     new ExactMethodMatcher("<init>", "(ZLjava/lang/String;Ljava/lang/String;)V"));
    }

    protected boolean isDispatcher() {
        return true;
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, final Object config, Object[] args) {
        ServiceFactory.getJmxService().createMBeanServerIfNeeded();
        return new MethodExitTracer(sig, transaction) {
            protected void doFinish(int opcode, Object returnValue) {
                try {
                    Field enabledField = config.getClass().getDeclaredField("enabled");
                    enabledField.setAccessible(true);
                    boolean enabled = enabledField.getBoolean(config);
                    if (!enabled) {
                        enabledField.setBoolean(config, true);
                        Agent.LOG.log(Level.INFO, "Enabling Solr Jmx metrics");
                    }
                } catch (Exception e) {
                    Agent.LOG.log(Level.SEVERE, "Unable to access the Solr JmxConfiguration enabled field");
                    Agent.LOG.log(Level.FINER, "Solr Jmx error", e);
                }
            }
        };
    }
}