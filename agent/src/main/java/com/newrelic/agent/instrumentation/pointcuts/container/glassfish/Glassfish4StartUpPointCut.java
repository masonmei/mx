package com.newrelic.agent.instrumentation.pointcuts.container.glassfish;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.jmx.values.GlassfishJmxValues;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.MethodExitTracer;
import com.newrelic.agent.tracers.Tracer;

@PointCut
public class Glassfish4StartUpPointCut extends TracerFactoryPointCut {
    private final AtomicBoolean addedJmx = new AtomicBoolean(false);

    public Glassfish4StartUpPointCut(ClassTransformer classTransformer) {
        super(new PointCutConfiguration(Glassfish4StartUpPointCut.class.getName(), "glassfish_instrumentation", true),
                     createClassMatcher(), createMethodMatcher());
    }

    private static ClassMatcher createClassMatcher() {
        return new ExactClassMatcher("com/sun/appserv/server/util/Version");
    }

    private static MethodMatcher createMethodMatcher() {
        return createExactMethodMatcher("getMajorVersion", new String[] {"()Ljava/lang/String;"});
    }

    protected boolean isDispatcher() {
        return true;
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args) {
        if (!addedJmx.get()) {
            return new MethodExitTracer(sig, transaction) {
                protected void doFinish(int opcode, Object returnValue) {
                    try {
                        if ((returnValue instanceof String)) {
                            String majorVersion = ((String) returnValue).trim();
                            if (majorVersion.length() > 0) {
                                Glassfish4StartUpPointCut.this.addJMX(majorVersion);
                            }
                        }
                    } catch (Exception e) {
                        if (Agent.LOG.isFinestEnabled()) {
                            Agent.LOG.log(Level.FINER, "Glassfish Jmx error", e);
                        }
                    }
                }
            };
        }

        return null;
    }

    private void addJMX(String majorVersion) {
        if ((!"1".equals(majorVersion)) && (!"2".equals(majorVersion)) && (!"3".equals(majorVersion))) {
            ServiceFactory.getJmxService().addJmxFrameworkValues(new GlassfishJmxValues());
            addedJmx.set(true);
            if (Agent.LOG.isFinerEnabled()) {
                Agent.LOG.log(Level.FINER,
                                     MessageFormat.format("Added JMX for Glassfish {0}", new Object[] {majorVersion}));
            }
        }
    }
}