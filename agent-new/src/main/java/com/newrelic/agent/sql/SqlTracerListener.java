package com.newrelic.agent.sql;

import com.newrelic.agent.instrumentation.pointcuts.database.SqlStatementTracer;
import java.util.List;

public abstract interface SqlTracerListener
{
  public abstract void noticeSqlTracer(SqlStatementTracer paramSqlStatementTracer);

  public abstract List<SqlStatementInfo> getSqlStatementInfo();
}