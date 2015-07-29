package com.newrelic.agent.tracers;

import com.newrelic.agent.Transaction;

public final class IgnoreTransactionTracerFactory extends AbstractTracerFactory
{
  public static final String TRACER_FACTORY_NAME = IgnoreTransactionTracerFactory.class.getName();

  public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args)
  {
    transaction.setIgnore(true);

    return new MethodExitTracerNoSkip(sig, transaction)
    {
      protected void doFinish(int opcode, Object returnValue)
      {
      }
    };
  }
}