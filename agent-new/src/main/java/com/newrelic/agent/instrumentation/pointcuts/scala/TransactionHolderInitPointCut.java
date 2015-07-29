package com.newrelic.agent.instrumentation.pointcuts.scala;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionState;
import com.newrelic.agent.async.AsyncTransactionState;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OrClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.NameMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;
import com.newrelic.agent.instrumentation.pointcuts.TransactionHolder;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import java.lang.reflect.Field;
import java.text.MessageFormat;

@com.newrelic.agent.instrumentation.pointcuts.PointCut
public class TransactionHolderInitPointCut extends com.newrelic.agent.instrumentation.PointCut
  implements EntryInvocationHandler
{
  public static final boolean DEFAULT_ENABLED = true;
  private static final String POINT_CUT_NAME = TransactionHolderInitPointCut.class.getName();
  private final boolean tracerNamingEnabled;

  public TransactionHolderInitPointCut(ClassTransformer classTransformer)
  {
    super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
    this.tracerNamingEnabled = ServiceFactory.getConfigService().getTransactionTracerConfig(null).isStackBasedNamingEnabled();
  }

  private static PointCutConfiguration createPointCutConfig() {
    return new PointCutConfiguration(POINT_CUT_NAME, "scala_instrumentation", true);
  }

  private static ClassMatcher createClassMatcher()
  {
    return OrClassMatcher.getClassMatcher(new ClassMatcher[] { new ExactClassMatcher("scala/concurrent/impl/AbstractPromise"), new ExactClassMatcher("scala/concurrent/impl/CallbackRunnable") });
  }

  private static MethodMatcher createMethodMatcher()
  {
    return new NameMethodMatcher("<init>");
  }

  protected PointCutInvocationHandler getPointCutInvocationHandlerImpl()
  {
    return this;
  }

  public void handleInvocation(ClassMethodSignature sig, Object object, Object[] args)
  {
    if ((object instanceof TransactionHolder)) {
      Transaction tx = Transaction.getTransaction();
      TransactionHolder th = (TransactionHolder)object;
      if ((tx.isStarted()) && (th._nr_getTransaction() == null)) {
        if ((!(tx.getTransactionState() instanceof AsyncTransactionState)) || (tx.getRootTransaction().isIgnore())) {
          return;
        }
        th._nr_setTransaction(tx);
        if (this.tracerNamingEnabled)
        {
          th._nr_setName(findTxName(th));
        }
        tx.getTransactionState().asyncJobStarted(th);
      }
    }
  }

  private String findTxName(TransactionHolder th) {
    if ((th instanceof CallbackRunnable))
    {
      Object onComplete = ((CallbackRunnable)th).onComplete();
      return analyzeOnComplete(onComplete);
    }

    for (StackTraceElement st : Thread.currentThread().getStackTrace())
      if (!st.getClassName().startsWith("com.newrelic.agent."))
      {
        if (!st.getClassName().startsWith("com.newrelic.bootstrap."))
        {
          if (!st.getClassName().startsWith("com.newrelic.api.agent."))
          {
            if (!st.getMethodName().equals("<init>"))
            {
              if (!st.getClassName().startsWith("scala.concurrent"))
              {
                if (!st.getClassName().startsWith("scala.collection"))
                {
                  if (!st.getClassName().startsWith("play.api.libs.concurrent"))
                  {
                    if (!st.getClassName().startsWith("play.api.libs.iteratee"))
                    {
                      if (!st.getClassName().startsWith("play.libs.F"))
                      {
                        if (!st.getClassName().startsWith("akka.pattern.PromiseActorRef"))
                        {
                          if (!st.getClassName().startsWith("java.util.concurrent.ThreadPoolExecutor"))
                          {
                            if (!st.getClassName().startsWith("java.lang.Thread"))
                            {
                              return MessageFormat.format("Java/{0}/{1}", new Object[] { st.getClassName(), st.getMethodName() });
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    return null;
  }

  private String analyzeOnComplete(Object onComplete) {
    for (Field field : onComplete.getClass().getDeclaredFields())
    {
      if (field.getType().getName().startsWith("scala.Function")) {
        boolean accessible = field.isAccessible();
        field.setAccessible(true);
        try {
          Object callback = field.get(onComplete);
          if ((callback.getClass().getName().startsWith("scala.concurrent")) || (callback.getClass().getName().startsWith("play.api.libs.concurrent")) || (callback.getClass().getName().startsWith("play.api.libs.iteratee")))
          {
            return analyzeOnComplete(callback);
          }
          return MessageFormat.format("Java/{0}/apply", new Object[] { callback.getClass().getName() });
        }
        catch (Exception e)
        {
          String str;
          return null;
        } finally {
          field.setAccessible(accessible);
        }
      }
    }
    return MessageFormat.format("Java/{0}/apply", new Object[] { onComplete.getClass().getName() });
  }

  @InterfaceMixin(originalClassName={"scala/concurrent/impl/CallbackRunnable"})
  public static abstract interface CallbackRunnable
  {
    @FieldAccessor(fieldName="onComplete", fieldDesc="Lscala/Function1;", existingField=true)
    public abstract Object onComplete();
  }
}