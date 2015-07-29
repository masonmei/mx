package com.newrelic.agent.instrumentation.pointcuts.database;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.service.ServiceFactory;

@PointCut
public class GenericPreparedStatementPointCut extends AbstractPreparedStatementPointCut
{
  public static final String PREPARED_STATEMENT_INTERFACE = "java/sql/PreparedStatement";

  public GenericPreparedStatementPointCut(ClassTransformer classTransformer)
  {
    super(new PointCutConfiguration("jdbc_prepared_statement", null, ServiceFactory.getConfigService().getDefaultAgentConfig().isGenericJDBCSupportEnabled()), new InterfaceMatcher("java/sql/PreparedStatement"));

    setPriority(-2147483648);
  }
}