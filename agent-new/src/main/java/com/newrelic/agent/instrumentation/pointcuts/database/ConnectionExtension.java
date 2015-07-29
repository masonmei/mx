package com.newrelic.agent.instrumentation.pointcuts.database;

import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;

public abstract interface ConnectionExtension {
    @FieldAccessor(fieldName = "connectionFactory")
    public abstract ConnectionFactory _nr_getConnectionFactory();

    @FieldAccessor(fieldName = "connectionFactory")
    public abstract void _nr_setConnectionFactory(ConnectionFactory paramConnectionFactory);
}