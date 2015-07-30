package com.newrelic.agent.instrumentation.pointcuts.database;

import java.sql.Connection;

import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName = {"com/newrelic/agent/deps/org/apache/commons/dbcp/DelegatingConnection"})
public abstract interface DelegatingConnection {
    public abstract Connection getInnermostDelegate();

    public abstract Connection getDelegate();

    @InterfaceMixin(originalClassName = {"com/newrelic/agent/deps/org/apache/commons/dbcp"
                                                 + "/PoolingDataSource$PoolGuardConnectionWrapper"})
    public static abstract interface PoolGuardConnectionWrapper {
        @FieldAccessor(fieldName = "delegate", existingField = true)
        public abstract Connection _nr_getDelegate();
    }
}