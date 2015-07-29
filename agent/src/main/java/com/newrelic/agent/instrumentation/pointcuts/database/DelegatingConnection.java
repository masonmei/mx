package com.newrelic.agent.instrumentation.pointcuts.database;

import java.sql.Connection;

import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName = {"com/newrelic/deps/org/apache/commons/dbcp/DelegatingConnection"})
public interface DelegatingConnection {
    Connection getInnermostDelegate();

    Connection getDelegate();

    @InterfaceMixin(originalClassName = {"com/newrelic/deps/org/apache/commons/dbcp/PoolingDataSource$PoolGuardConnectionWrapper"})
    interface PoolGuardConnectionWrapper {
        @FieldAccessor(fieldName = "delegate", existingField = true)
        Connection _nr_getDelegate();
    }
}