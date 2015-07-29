package com.newrelic.agent;

import com.newrelic.agent.instrumentation.pointcuts.TransactionHolder;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFactory;

public interface TransactionState {
    Tracer getTracer(Transaction paramTransaction, TracerFactory paramTracerFactory,
                     ClassMethodSignature paramClassMethodSignature, Object paramObject, Object[] paramArrayOfObject);

    Tracer getTracer(Transaction paramTransaction, String paramString, ClassMethodSignature paramClassMethodSignature,
                     Object paramObject, Object[] paramArrayOfObject);

    Tracer getTracer(Transaction paramTransaction, Object paramObject, ClassMethodSignature paramClassMethodSignature,
                     String paramString, int paramInt);

    boolean finish(Transaction paramTransaction, Tracer paramTracer);

    void resume();

    void suspend();

    void suspendRootTracer();

    void complete();

    void asyncJobStarted(TransactionHolder paramTransactionHolder);

    void asyncJobFinished(TransactionHolder paramTransactionHolder);

    void asyncTransactionStarted(Transaction paramTransaction, TransactionHolder paramTransactionHolder);

    void asyncTransactionFinished(TransactionActivity paramTransactionActivity);

    void mergeAsyncTracers();

    Tracer getRootTracer();

    void asyncJobInvalidate(TransactionHolder paramTransactionHolder);

    void setInvalidateAsyncJobs(boolean paramBoolean);
}