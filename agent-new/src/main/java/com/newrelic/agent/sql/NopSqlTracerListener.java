package com.newrelic.agent.sql;

import java.util.Collections;
import java.util.List;

import com.newrelic.agent.instrumentation.pointcuts.database.SqlStatementTracer;

public class NopSqlTracerListener implements SqlTracerListener {
    public void noticeSqlTracer(SqlStatementTracer sqlTracer) {
    }

    public List<SqlStatementInfo> getSqlStatementInfo() {
        return Collections.emptyList();
    }
}