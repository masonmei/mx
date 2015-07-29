package com.newrelic.agent.instrumentation.pointcuts.database;

import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;

@PointCut
public class JTDSPreparedStatementPointCut extends AbstractPreparedStatementPointCut
{
  private static final String JTDS_PREPARED_STATEMENT_MATCH = "net/sourceforge/jtds/jdbc/JtdsPreparedStatement";

  public JTDSPreparedStatementPointCut(ClassTransformer classTransformer)
  {
    super(new PointCutConfiguration("jdbc_jtds_prepared_statement", null, true), new ExactClassMatcher("net/sourceforge/jtds/jdbc/JtdsPreparedStatement"));
  }
}