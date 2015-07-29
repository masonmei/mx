package com.newrelic.agent.instrumentation.pointcuts.database;

import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;

@PointCut
public class PostgresStatementPointCut extends AbstractPreparedStatementPointCut
{
  static final ExactClassMatcher POSTGRESQL_STATEMENT_CLASS_MATCHER = new ExactClassMatcher("org/postgresql/jdbc2/AbstractJdbc2Statement");

  public PostgresStatementPointCut(ClassTransformer classTransformer)
  {
    super(new PointCutConfiguration(PostgresStatementPointCut.class), POSTGRESQL_STATEMENT_CLASS_MATCHER);
  }
}