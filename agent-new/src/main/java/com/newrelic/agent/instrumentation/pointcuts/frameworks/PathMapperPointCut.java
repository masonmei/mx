//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts.frameworks;

import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.MethodExitTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.transaction.TransactionNamingPolicy;

@PointCut
public class PathMapperPointCut extends TracerFactoryPointCut {
  private static final String SITEMESH = "SiteMesh";

  public PathMapperPointCut(ClassTransformer classTransformer) {
    super(new PointCutConfiguration(PathMapperPointCut.class.getName(), (String) null, false),
                 new ExactClassMatcher("com/opensymphony/module/sitemesh/mapper/PathMapper"),
                 new ExactMethodMatcher("findKey", "(Ljava/lang/String;Ljava/util/Map;)Ljava/lang/String;"));
  }

  public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args) {
    return new PathMapperPointCut.PathMapperTracer(transaction, sig, object, (String) args[0]);
  }

  private static class PathMapperTracer extends MethodExitTracer {
    public PathMapperTracer(Transaction transaction, ClassMethodSignature sig, Object mapper, String path) {
      super(sig, transaction);
    }

    protected void doFinish(int opcode, Object key) {
      if (key != null) {
        Agent.LOG.finer("Normalizing path using SiteMesh config");
        String path = key.toString();
        if (!path.startsWith("/")) {
          path = "/" + path;
        }

        this.setTransactionName(this.getTransaction(), path);
      }

    }

    private void setTransactionName(Transaction transaction, String path) {
      if (transaction.isTransactionNamingEnabled()) {
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        if (Agent.LOG.isLoggable(Level.FINER) && policy.canSetTransactionName(transaction,
                                                                                     TransactionNamePriority
                                                                                             .FRAMEWORK)) {
          String msg = MessageFormat.format("Setting transaction name to \"{0}\" using SiteMesh config",
                                                   new Object[] {path});
          Agent.LOG.finer(msg);
        }

        policy.setTransactionName(transaction, path, "SiteMesh", TransactionNamePriority.FRAMEWORK);
      }
    }
  }
}
