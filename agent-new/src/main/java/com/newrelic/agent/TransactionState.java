package com.newrelic.agent;

import com.newrelic.agent.instrumentation.pointcuts.TransactionHolder;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFactory;

public abstract interface TransactionState
{
  public abstract Tracer getTracer(Transaction paramTransaction, TracerFactory paramTracerFactory,
                                   ClassMethodSignature paramClassMethodSignature, Object paramObject,
                                   Object[] paramArrayOfObject);

  public abstract Tracer getTracer(Transaction paramTransaction, String paramString,
                                   ClassMethodSignature paramClassMethodSignature, Object paramObject,
                                   Object[] paramArrayOfObject);

  public abstract Tracer getTracer(Transaction paramTransaction, Object paramObject,
                                   ClassMethodSignature paramClassMethodSignature, String paramString, int paramInt);

  public abstract boolean finish(Transaction paramTransaction, Tracer paramTracer);

  public abstract void resume();

  public abstract void suspend();

  public abstract void suspendRootTracer();

  public abstract void complete();

  public abstract void asyncJobStarted(TransactionHolder paramTransactionHolder);

  public abstract void asyncJobFinished(TransactionHolder paramTransactionHolder);

  public abstract void asyncTransactionStarted(Transaction paramTransaction, TransactionHolder paramTransactionHolder);

  public abstract void asyncTransactionFinished(TransactionActivity paramTransactionActivity);

  public abstract void mergeAsyncTracers();

  public abstract Tracer getRootTracer();

  public abstract void asyncJobInvalidate(TransactionHolder paramTransactionHolder);

  public abstract void setInvalidateAsyncJobs(boolean paramBoolean);
}