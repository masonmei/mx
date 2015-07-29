package com.newrelic.agent.tracers;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;

public abstract class ExternalComponentPointCut extends TracerFactoryPointCut
{
  public ExternalComponentPointCut(PointCutConfiguration config, ClassMatcher classMatcher, MethodMatcher methodMatcher)
  {
    super(config, classMatcher, methodMatcher);
  }

  public final Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args)
  {
    Tracer parent = transaction.getTransactionActivity().getLastTracer();
    if ((parent != null) && ((parent instanceof IgnoreChildSocketCalls))) {
      return null;
    }
    return getExternalTracer(transaction, sig, object, args);
  }

  protected abstract Tracer getExternalTracer(Transaction paramTransaction, ClassMethodSignature paramClassMethodSignature, Object paramObject, Object[] paramArrayOfObject);
}