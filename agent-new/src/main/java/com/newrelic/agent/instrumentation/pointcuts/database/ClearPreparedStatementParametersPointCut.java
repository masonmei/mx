package com.newrelic.agent.instrumentation.pointcuts.database;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;

@com.newrelic.agent.instrumentation.pointcuts.PointCut
public class ClearPreparedStatementParametersPointCut extends com.newrelic.agent.instrumentation.PointCut
  implements EntryInvocationHandler
{
  private static final String CLEAR_PARAMETERS_METHOD_NAME = "clearParameters";
  private static final MethodMatcher METHOD_MATCHER = new ExactMethodMatcher("clearParameters", "()V");

  public ClearPreparedStatementParametersPointCut(ClassTransformer classTransformer) {
    super(new PointCutConfiguration("jdbc_parameterized_prepared_statement", null, ServiceFactory.getConfigService().getDefaultAgentConfig().isGenericJDBCSupportEnabled()), ParameterizedPreparedStatementPointCut.createClassMatcher(), METHOD_MATCHER);
  }

  public void handleInvocation(ClassMethodSignature sig, Object statement, Object[] args)
  {
    if ((statement instanceof PreparedStatementExtension)) {
      PreparedStatementExtension preparedStatement = (PreparedStatementExtension)statement;
      Object[] params = preparedStatement._nr_getSqlParameters();
      if (params != null)
        for (int i = 0; i < params.length; i++)
          params[i] = null;
    }
  }

  protected PointCutInvocationHandler getPointCutInvocationHandlerImpl()
  {
    return this;
  }
}