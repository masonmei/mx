package com.newrelic.agent.instrumentation.pointcuts.database;

import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;

public abstract interface PreparedStatementExtension {
    @FieldAccessor(fieldName = "sqlParameters")
    public abstract void _nr_setSqlParameters(Object[] paramArrayOfObject);

    @FieldAccessor(fieldName = "sqlParameters")
    public abstract Object[] _nr_getSqlParameters();

    @FieldAccessor(fieldName = "statementData")
    public abstract StatementData _nr_getStatementData();

    @FieldAccessor(fieldName = "statementData")
    public abstract void _nr_setStatementData(StatementData paramStatementData);
}