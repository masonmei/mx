package com.newrelic.agent.instrumentation.pointcuts.scala;

import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;
import com.newrelic.agent.instrumentation.pointcuts.TransactionHolder;

@InterfaceMixin(originalClassName = {"scala/concurrent/impl/CallbackRunnable", "scala/concurrent/impl/AbstractPromise"})
public interface ScalaTransactionHolder extends TransactionHolder {
    String CALLBACK_RUNNABLE_CLASS = "scala/concurrent/impl/CallbackRunnable";
    String PROMISE_CLASS = "scala/concurrent/impl/Promise$DefaultPromise";
    String PROMISE_ABSTRACT_CLASS = "scala/concurrent/impl/AbstractPromise";
    String PROMISE_INTERFACE = "scala/concurrent/Promise";

    @FieldAccessor(fieldName = "transaction", volatileAccess = true)
    void _nr_setTransaction(Object paramObject);

    @FieldAccessor(fieldName = "transaction", volatileAccess = true)
    Object _nr_getTransaction();

    @FieldAccessor(fieldName = "name", volatileAccess = true)
    void _nr_setName(Object paramObject);

    @FieldAccessor(fieldName = "name", volatileAccess = true)
    Object _nr_getName();
}