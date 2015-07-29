package com.newrelic.agent.instrumentation.pointcuts.database;

import java.sql.PreparedStatement;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.regex.Pattern;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;

public abstract class AbstractPreparedStatementPointCut extends TracerFactoryPointCut {
    static final MethodMatcher METHOD_MATCHER = OrMethodMatcher
                                                        .getMethodMatcher(new MethodMatcher[] {new ExactMethodMatcher
                                                                                                       ("executeQuery",
                                                                                                                             "()Ljava/sql/ResultSet;"),
                                                                                                      new ExactMethodMatcher("executeUpdate",
                                                                                                                                    "()I"),
                                                                                                      new ExactMethodMatcher("execute",
                                                                                                                                    "()Z")});
    private static final String PARAMETER_REGEX = "\\?";
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\?");
    private final IAgentLogger logger;

    protected AbstractPreparedStatementPointCut(PointCutConfiguration config, ClassMatcher classMatcher) {
        super(config, classMatcher, METHOD_MATCHER);
        this.logger = Agent.LOG.getChildLogger(getClass());
    }

    public static String parameterizeSql(String sql, Object[] parameters) throws Exception {
        if ((sql == null) || (parameters == null) || (parameters.length == 0)) {
            return sql;
        }
        String[] pieces = PARAMETER_PATTERN.split(sql);
        StringBuilder sb = new StringBuilder(sql.length() * 2);
        int i = 0;
        for (int j = 1; i < pieces.length; j++) {
            String piece = pieces[i];
            if ((j == pieces.length) && (sql.endsWith(piece))) {
                sb.append(piece);
            } else {
                Object val = i < parameters.length ? parameters[i] : null;
                if ((val instanceof Number)) {
                    sb.append(piece).append(val.toString());
                } else if (val == null) {
                    sb.append(piece).append("?");
                } else {
                    sb.append(piece).append("'").append(val.toString()).append("'");
                }
            }
            i++;
        }

        return sb.toString();
    }

    protected IAgentLogger getLogger() {
        return this.logger;
    }

    public final Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object preparedStatement,
                                    Object[] args) {
        if ((preparedStatement instanceof PreparedStatementExtension)) {
            StatementData statementData = ((PreparedStatementExtension) preparedStatement)._nr_getStatementData();

            if (statementData != null) {
                if (this.logger.isLoggable(Level.FINEST)) {
                    String msg = MessageFormat.format("Created PreparedStatementTracer for: {0}",
                                                             new Object[] {preparedStatement.getClass().getName()});

                    this.logger.finest(msg);
                }
                return new PreparedStatementTracer(transaction, sig, (PreparedStatementExtension) preparedStatement,
                                                          statementData);
            }

            if (this.logger.isLoggable(Level.FINEST)) {
                try {
                    String msg = MessageFormat.format("Statement data is null: {0},{1}", new Object[] {sig,
                                                                                                              ((PreparedStatement) preparedStatement)
                                                                                                                      .getConnection()
                                                                                                                      .getClass()
                                                                                                                      .getName()});

                    this.logger.finest(msg);
                } catch (Throwable ex) {
                    this.logger.finest(MessageFormat.format("Statement data is null: {0}", new Object[] {sig}));
                }
            }

        } else if (this.logger.isLoggable(Level.FINEST)) {
            String msg = MessageFormat.format("PreparedStatement does not implement PreparedStatementExtension: {0}",
                                                     new Object[] {preparedStatement.getClass().getName()});

            this.logger.finest(msg);
        }

        return null;
    }
}