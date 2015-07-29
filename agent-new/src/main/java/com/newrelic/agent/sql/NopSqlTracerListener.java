package com.newrelic.agent.sql;

import com.newrelic.agent.instrumentation.pointcuts.database.SqlStatementTracer;
import java.util.Collections;
import java.util.List;

public class NopSqlTracerListener
  implements SqlTracerListener
{
  public void noticeSqlTracer(SqlStatementTracer sqlTracer)
  {
  }

  public List<SqlStatementInfo> getSqlStatementInfo()
  {
    return Collections.emptyList();
  }
}