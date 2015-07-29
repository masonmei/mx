package com.newrelic.agent.instrumentation.pointcuts.akka;

import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;
import com.newrelic.agent.instrumentation.pointcuts.TransactionHolder;

@InterfaceMixin(originalClassName = {"akka/dispatch/Envelope", "akka/dispatch/Future$$anon$4",
                                            "akka/dispatch/AbstractPromise"})
public abstract interface AkkaTransactionHolder extends TransactionHolder {
    public static final String CLASS = "akka/dispatch/Envelope";
    public static final String CALLBACK_RUNNABLE_CLASS = "akka/dispatch/Future$$anon$4";
    public static final String PROMISE_CLASS = "akka/dispatch/DefaultPromise";
    public static final String PROMISE_ABSTRACT_CLASS = "akka/dispatch/AbstractPromise";
    public static final String PROMISE_INTERFACE = "akka/dispatch/Promise";

    @FieldAccessor(fieldName = "transaction", volatileAccess = true)
    public abstract void _nr_setTransaction(Object paramObject);

    @FieldAccessor(fieldName = "transaction", volatileAccess = true)
    public abstract Object _nr_getTransaction();

    @FieldAccessor(fieldName = "name", volatileAccess = true)
    public abstract void _nr_setName(Object paramObject);

    @FieldAccessor(fieldName = "name", volatileAccess = true)
    public abstract Object _nr_getName();
}