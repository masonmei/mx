package com.newrelic.agent.instrumentation.pointcuts.database;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import com.newrelic.agent.tracers.TracerFactory;

@com.newrelic.agent.instrumentation.pointcuts.PointCut
public class MySQLCreatePreparedStatementPointCut extends com.newrelic.agent.instrumentation.PointCut
{
  private static final String MYSQL_CONNECTION_CLASS = "com/mysql/jdbc/ConnectionImpl";
  private static final MethodMatcher METHOD_MATCHER = OrMethodMatcher.getMethodMatcher(new MethodMatcher[] { new ExactMethodMatcher("clientPrepareStatement", new String[] { "(Ljava/lang/String;)Ljava/sql/PreparedStatement;", "(Ljava/lang/String;III)Ljava/sql/PreparedStatement;", "(Ljava/lang/String;II)Ljava/sql/PreparedStatement;", "(Ljava/lang/String;I)Ljava/sql/PreparedStatement;", "(Ljava/lang/String;[I)Ljava/sql/PreparedStatement;", "(Ljava/lang/String;[Ljava/lang/String;)Ljava/sql/PreparedStatement;" }), new ExactMethodMatcher("serverPrepareStatement", new String[] { "(Ljava/lang/String;)Ljava/sql/PreparedStatement;", "(Ljava/lang/String;III)Ljava/sql/PreparedStatement;", "(Ljava/lang/String;II)Ljava/sql/PreparedStatement;", "(Ljava/lang/String;I)Ljava/sql/PreparedStatement;", "(Ljava/lang/String;[I)Ljava/sql/PreparedStatement;", "(Ljava/lang/String;[Ljava/lang/String;)Ljava/sql/PreparedStatement;" }), CreatePreparedStatementPointCut.METHOD_MATCHER });
  private final TracerFactory tracerFactory;

  public MySQLCreatePreparedStatementPointCut(ClassTransformer classTransformer)
  {
    this(ServiceFactory.getConfigService().getDefaultAgentConfig());
  }

  private MySQLCreatePreparedStatementPointCut(AgentConfig config) {
    super(new PointCutConfiguration("jdbc_prepare_statement", null, true), new ExactClassMatcher("com/mysql/jdbc/ConnectionImpl"), METHOD_MATCHER);

    this.tracerFactory = new CreatePreparedStatementTracerFactory();
  }

  protected PointCutInvocationHandler getPointCutInvocationHandlerImpl()
  {
    return this.tracerFactory;
  }
}