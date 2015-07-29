package com.newrelic.agent.instrumentation.pointcuts.frameworks.struts;

import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.MethodExitTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.transaction.TransactionNamingPolicy;
import com.newrelic.agent.util.Invoker;

@PointCut
public class StrutsActionConfigMatcherPointCut extends TracerFactoryPointCut {
    private static final String STRUTS = "Struts";
    private static final String ACTION_CONFIG_MATCHER_CLASS = "org/apache/struts/config/ActionConfigMatcher";
    private static final String GET_PATH = "getPath";

    public StrutsActionConfigMatcherPointCut(ClassTransformer classTransformer) {
        super(StrutsActionConfigMatcherPointCut.class,
                     new ExactClassMatcher("org/apache/struts/config/ActionConfigMatcher"),
                     createExactMethodMatcher("convertActionConfig", new String[] {"(Ljava/lang/String;" +
                                                                                           "Lorg/apache/struts/config/ActionConfig;"
                                                                                           + "Ljava/util/Map;)" +
                                                                                           "Lorg/apache/struts/config/ActionConfig;"
                                                                                           + ""}));
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object matcher, Object[] args) {
        return new StrutsActionConfigMatcherPointCut.StrutsActionConfigMatcherTracer(transaction, sig, matcher, args);
    }

    private static class StrutsActionConfigMatcherTracer extends MethodExitTracer {
        public StrutsActionConfigMatcherTracer(Transaction transaction, ClassMethodSignature sig, Object matcher,
                                               Object[] args) {
            super(sig, transaction);

            String msg;
            try {
                Object e = args[1];
                msg = (String) Invoker.invoke(e, e.getClass(), "getPath", new Object[0]);
                Agent.LOG.finer("Normalizing path using Struts wildcard");
                this.setTransactionName(transaction, msg);
            } catch (Exception var7) {
                msg = MessageFormat.format("Exception in {0} handling {1}: {2}",
                                                  new Object[] {StrutsActionConfigMatcherPointCut.class.getSimpleName(),
                                                                       sig, var7});
                if (Agent.LOG.isLoggable(Level.FINEST)) {
                    Agent.LOG.log(Level.FINEST, msg, var7);
                } else {
                    Agent.LOG.finer(msg);
                }
            }

        }

        private void setTransactionName(Transaction transaction, String wildcardPath) {
            if (transaction.isTransactionNamingEnabled()) {
                TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
                if (Agent.LOG.isLoggable(Level.FINER) && policy.canSetTransactionName(transaction,
                                                                                             TransactionNamePriority
                                                                                                     .FRAMEWORK)) {
                    String msg = MessageFormat.format("Setting transaction name to \"{0}\" using Stuts wildcard",
                                                             new Object[] {wildcardPath});
                    Agent.LOG.finer(msg);
                }

                policy.setTransactionName(transaction, wildcardPath, "Struts", TransactionNamePriority.FRAMEWORK);
            }
        }

        protected void doFinish(int opcode, Object returnValue) {
        }
    }
}
