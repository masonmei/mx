package com.newrelic.agent.sql;

import java.util.List;

import com.newrelic.agent.instrumentation.pointcuts.database.SqlStatementTracer;

public abstract interface SqlTracerListener {
    public abstract void noticeSqlTracer(SqlStatementTracer paramSqlStatementTracer);

    public abstract List<SqlStatementInfo> getSqlStatementInfo();
}