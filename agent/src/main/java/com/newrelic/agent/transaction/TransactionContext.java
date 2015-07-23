package com.newrelic.agent.transaction;

public abstract interface TransactionContext {
    public abstract void _nr_setTransaction(Object paramObject);

    public abstract Object _nr_getTransaction();
}