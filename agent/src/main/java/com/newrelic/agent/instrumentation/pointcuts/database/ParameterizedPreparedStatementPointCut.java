package com.newrelic.agent.instrumentation.pointcuts.database;

import java.text.MessageFormat;
import java.util.Set;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.JDBCClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;

@com.newrelic.agent.instrumentation.pointcuts.PointCut
public class ParameterizedPreparedStatementPointCut extends com.newrelic.agent.instrumentation.PointCut implements
        EntryInvocationHandler {
    static final String NAME = "jdbc_parameterized_prepared_statement";
    private static final String SET_INT_METHOD_NAME = "setInt";
    private static final String SET_NULL_METHOD_NAME = "setNull";
    private static final String SET_BOOLEAN_METHOD_NAME = "setBoolean";
    private static final String SET_BYTE_METHOD_NAME = "setByte";
    private static final String SET_SHORT_METHOD_NAME = "setShort";
    private static final String SET_LONG_METHOD_NAME = "setLong";
    private static final String SET_FLOAT_METHOD_NAME = "setFloat";
    private static final String SET_DOUBLE_METHOD_NAME = "setDouble";
    private static final String SET_BIG_DECIMAL_METHOD_NAME = "setBigDecimal";
    private static final String SET_STRING_METHOD_NAME = "setString";
    private static final String SET_DATE_METHOD_NAME = "setDate";
    private static final String SET_TIME_METHOD_NAME = "setTime";
    private static final String SET_TIMESTAMP_METHOD_NAME = "setTimestamp";
    private static final MethodMatcher METHOD_MATCHER = OrMethodMatcher
                                                                .getMethodMatcher(new MethodMatcher[] {new ExactMethodMatcher("setInt",
                                                                                                                                     "(II)V"),
                                                                                                              new ExactMethodMatcher("setBoolean",
                                                                                                                                            "(IZ)V"),
                                                                                                              new ExactMethodMatcher("setByte",
                                                                                                                                            "(IB)V"),
                                                                                                              new ExactMethodMatcher("setShort",
                                                                                                                                            "(IS)V"),
                                                                                                              new ExactMethodMatcher("setLong",
                                                                                                                                            "(IJ)V"),
                                                                                                              new ExactMethodMatcher("setFloat",
                                                                                                                                            "(IF)V"),
                                                                                                              new ExactMethodMatcher("setDouble",
                                                                                                                                            "(ID)V"),
                                                                                                              new ExactMethodMatcher("setBigDecimal",
                                                                                                                                            "(ILjava/math/BigDecimal;)V"),
                                                                                                              new ExactMethodMatcher("setString",
                                                                                                                                            "(ILjava/lang/String;)V"),
                                                                                                              new ExactMethodMatcher("setDate",
                                                                                                                                            "(ILjava/sql/Date;)V"),
                                                                                                              new ExactMethodMatcher("setTime",
                                                                                                                                            "(ILjava/sql/Time;)V"),
                                                                                                              new ExactMethodMatcher("setTimestamp",
                                                                                                                                            "(ILjava/sql/Timestamp;)V"),
                                                                                                              new ExactMethodMatcher("setNull",
                                                                                                                                            "(II)V")});
    private final IAgentLogger logger;

    public ParameterizedPreparedStatementPointCut(ClassTransformer classTransformer) {
        super(new PointCutConfiguration("jdbc_parameterized_prepared_statement", null,
                                               ServiceFactory.getConfigService().getDefaultAgentConfig()
                                                       .isGenericJDBCSupportEnabled()), createClassMatcher(),
                     METHOD_MATCHER);

        logger = Agent.LOG.getChildLogger(getClass());
    }

    static final ClassMatcher createClassMatcher() {
        Set jdbcClasses = JDBCClassTransformer.getJdbcStatementClasses();
        return ExactClassMatcher.or((String[]) jdbcClasses.toArray(new String[0]));
    }

    static Object[] growParameterArray(Object[] params, int missingIndex) {
        int length = Math.max(10, (int) (missingIndex * 1.2D));
        Object[] newParams = new Object[length];
        System.arraycopy(params, 0, newParams, 0, params.length);

        return newParams;
    }

    public void handleInvocation(ClassMethodSignature sig, Object statement, Object[] args) {
        if ((statement instanceof PreparedStatementExtension)) {
            PreparedStatementExtension preparedStatement = (PreparedStatementExtension) statement;
            Object[] params = preparedStatement._nr_getSqlParameters();
            if (params != null) {
                try {
                    int index = ((Integer) args[0]).intValue();
                    index--;
                    Object value = args[1];
                    if (index < 0) {
                        logger.finer("Unable to store a prepared statement parameter because the index < 0");
                        return;
                    }
                    if (index >= params.length) {
                        params = growParameterArray(params, index);
                        preparedStatement._nr_setSqlParameters(params);
                    }
                    params[index] = value;
                } catch (Exception e) {
                    if (logger.isLoggable(Level.FINE)) {
                        String msg = MessageFormat.format("Instrumentation error for {0} in {1}: {2}",
                                                                 new Object[] {sig.toString(),
                                                                                      ParameterizedPreparedStatementPointCut.class
                                                                                              .getName(),
                                                                                      e.toString()});

                        if (logger.isLoggable(Level.FINEST)) {
                            logger.log(Level.FINEST, msg, e);
                        } else {
                            logger.log(Level.FINE, msg);
                        }
                    }
                }
            }
        }
    }

    protected PointCutInvocationHandler getPointCutInvocationHandlerImpl() {
        return this;
    }
}