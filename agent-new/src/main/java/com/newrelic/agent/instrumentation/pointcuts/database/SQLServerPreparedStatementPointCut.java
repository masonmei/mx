package com.newrelic.agent.instrumentation.pointcuts.database;

import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;

@PointCut
public class SQLServerPreparedStatementPointCut extends AbstractPreparedStatementPointCut
{
  private static final String SQLSERVER_PREPARED_STATEMENT_MATCH = "com/microsoft/sqlserver/jdbc/SQLServerPreparedStatement";

  public SQLServerPreparedStatementPointCut(ClassTransformer classTransformer)
  {
    super(new PointCutConfiguration("jdbc_sqlserver_prepared_statement", null, true), new ExactClassMatcher("com/microsoft/sqlserver/jdbc/SQLServerPreparedStatement"));
  }
}