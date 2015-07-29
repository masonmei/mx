//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts.frameworks.struts;

import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.transaction.TransactionNamingPolicy;

@PointCut
public class Struts2ActionPointCut extends TracerFactoryPointCut {
    public static final String STRUTS_ACTION__PROXY_INTERFACE = "com/opensymphony/xwork2/ActionProxy";
    private static final MethodMatcher METHOD_MATCHER =
            createExactMethodMatcher("execute", new String[] {"()Ljava/lang/String;"});

    public Struts2ActionPointCut(ClassTransformer classTransformer) {
        super(Struts2ActionPointCut.class, new InterfaceMatcher("com/opensymphony/xwork2/ActionProxy"), METHOD_MATCHER);
    }

    public Tracer doGetTracer(Transaction tx, ClassMethodSignature sig, Object action, Object[] args) {
        try {
            String e;
            if (action instanceof ActionProxy) {
                e = ((ActionProxy) action).getActionName();
            } else {
                e = (String) action.getClass().getMethod("getActionName", new Class[0]).invoke(action, new Object[0]);
            }

            this.setTransactionName(tx, e);
            return new DefaultTracer(tx, sig, action, new SimpleMetricNameFormat("StrutsAction/" + e));
        } catch (Exception var6) {
            return new DefaultTracer(tx, sig, action, new ClassMethodMetricNameFormat(sig, action, "StrutsAction"));
        }
    }

    private void setTransactionName(Transaction tx, String action) {
        if (tx.isTransactionNamingEnabled()) {
            TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
            if (Agent.LOG.isLoggable(Level.FINER) && policy.canSetTransactionName(tx,
                                                                                         TransactionNamePriority
                                                                                                 .FRAMEWORK)) {
                String msg = MessageFormat.format("Setting transaction name to \"{0}\" using Struts 2 action",
                                                         new Object[] {action});
                Agent.LOG.finer(msg);
            }

            policy.setTransactionName(tx, action, "StrutsAction", TransactionNamePriority.FRAMEWORK);
        }
    }
}
