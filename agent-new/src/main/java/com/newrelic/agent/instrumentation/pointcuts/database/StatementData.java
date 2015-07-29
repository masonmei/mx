package com.newrelic.agent.instrumentation.pointcuts.database;

import java.sql.Statement;

import com.newrelic.agent.database.ParsedDatabaseStatement;

public abstract interface StatementData {
    public abstract Statement getStatement();

    public abstract String getSql();

    public abstract ParsedDatabaseStatement getParsedStatement(Object paramObject, long paramLong);
}