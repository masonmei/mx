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
import com.newrelic.agent.instrumentation.classmatchers.ChildClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.transaction.TransactionNamingPolicy;

@PointCut
public class StrutsActionPointCut extends TracerFactoryPointCut {
  public static final String STRUTS_ACTION_CLASS = "org/apache/struts/action/Action";
  private static final MethodMatcher METHOD_MATCHER = createExactMethodMatcher("execute",
                                                                                      new String[] {"(Lorg/apache/struts/action/ActionMapping;Lorg/apache/struts/action/ActionForm;Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)Lorg/apache/struts/action/ActionForward;",
                                                                                                           "(Lorg/apache/struts/action/ActionMapping;Lorg/apache/struts/action/ActionForm;Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)Lorg/apache/struts/action/ActionForward;"});

  public StrutsActionPointCut(ClassTransformer classTransformer) {
    super(StrutsActionPointCut.class, new ChildClassMatcher("org/apache/struts/action/Action"), METHOD_MATCHER);
  }

  public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object action, Object[] args) {
    return new StrutsActionPointCut.StrutsActionTracer(transaction, sig, action, args);
  }

  private static class StrutsActionTracer extends DefaultTracer {
    private final String actionClassName;

    public StrutsActionTracer(Transaction transaction, ClassMethodSignature sig, Object action, Object[] args) {
      super(transaction, sig, action);
      this.actionClassName = action.getClass().getName();
      this.setTransactionName(transaction, this.actionClassName);
      this.setMetricNameFormat(new SimpleMetricNameFormat("StrutsAction/" + this.actionClassName));
    }

    private void setTransactionName(Transaction tx, String action) {
      if (tx.isTransactionNamingEnabled()) {
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        if (Agent.LOG.isLoggable(Level.FINER) && policy.canSetTransactionName(tx,
                                                                                     TransactionNamePriority.FRAMEWORK)) {
          String msg = MessageFormat.format("Setting transaction name to \"{0}\" using Struts action",
                                                   new Object[] {action});
          Agent.LOG.finer(msg);
        }

        policy.setTransactionName(tx, action, "StrutsAction", TransactionNamePriority.FRAMEWORK);
      }
    }
  }
}
