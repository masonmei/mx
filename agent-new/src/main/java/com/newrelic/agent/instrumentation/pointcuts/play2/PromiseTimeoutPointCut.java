package com.newrelic.agent.instrumentation.pointcuts.play2;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionState;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.MethodExitTracerNoSkip;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import com.newrelic.agent.tracers.Tracer;

@PointCut
public class PromiseTimeoutPointCut extends TracerFactoryPointCut
{
  public static final boolean DEFAULT_ENABLED = true;
  private static final String POINT_CUT_NAME = PromiseTimeoutPointCut.class.getName();
  private static final String CLASS_NAME = "play/api/libs/concurrent/Promise$";
  private static final String METHOD_NAME = "timeout";
  private static final String METHOD_DESC = "(Lscala/Function0;JLjava/util/concurrent/TimeUnit;Lscala/concurrent/ExecutionContext;)Lscala/concurrent/Future;";

  public PromiseTimeoutPointCut(ClassTransformer classTransformer)
  {
    super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
  }

  private static PointCutConfiguration createPointCutConfig() {
    return new PointCutConfiguration(POINT_CUT_NAME, "play2_instrumentation", true);
  }

  private static ClassMatcher createClassMatcher()
  {
    return new ExactClassMatcher("play/api/libs/concurrent/Promise$");
  }

  private static MethodMatcher createMethodMatcher() {
    return new ExactMethodMatcher("timeout", "(Lscala/Function0;JLjava/util/concurrent/TimeUnit;Lscala/concurrent/ExecutionContext;)Lscala/concurrent/Future;");
  }

  protected PointCutInvocationHandler getPointCutInvocationHandlerImpl()
  {
    return this;
  }

  public Tracer doGetTracer(final Transaction tx, ClassMethodSignature sig, Object object, Object[] args)
  {
    if (!tx.isStarted()) {
      return null;
    }
    tx.getTransactionState().setInvalidateAsyncJobs(true);
    return new MethodExitTracerNoSkip(sig, tx)
    {
      protected void doFinish(int opcode, Object returnValue)
      {
        tx.getTransactionState().setInvalidateAsyncJobs(false);
      }

      public void finish(Throwable throwable)
      {
        tx.getTransactionState().setInvalidateAsyncJobs(false);
      }
    };
  }
}