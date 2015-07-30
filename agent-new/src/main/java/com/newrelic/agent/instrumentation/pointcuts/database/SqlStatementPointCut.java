package com.newrelic.agent.instrumentation.pointcuts.database;

import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OrClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;

@PointCut
public class SqlStatementPointCut extends TracerFactoryPointCut {
    public static final String SQL_STATEMENT_CLASS = "java/sql/Statement";
    static final String MYSQL_STATEMENT_CLASS = "com/mysql/jdbc/Statement";
    static final String EXECUTE_METHOD_NAME = "execute";
    static final String EXECUTE_UPDATE_METHOD_NAME = "executeUpdate";
    static final String EXECUTE_QUERY_METHOD_NAME = "executeQuery";
    private static final String EXECUTE_QUERY_METHOD_DESC = "(Ljava/lang/String;)Ljava/sql/ResultSet;";
    private static final MethodMatcher METHOD_MATCHER = OrMethodMatcher
                                                                .getMethodMatcher(new MethodMatcher[] {new ExactMethodMatcher("execute",
                                                                                                                                     new String[] {"(Ljava/lang/String;)Z",
                                                                                                                                                          "(Ljava/lang/String;I)Z",
                                                                                                                                                          "(Ljava/lang/String;[I)Z",
                                                                                                                                                          "(Ljava/lang/String;[Ljava/lang/String;)Z"}),
                                                                                                              new ExactMethodMatcher("executeUpdate",
                                                                                                                                            new String[] {"(Ljava/lang/String;)I",
                                                                                                                                                                 "(Ljava/lang/String;I)I",
                                                                                                                                                                 "(Ljava/lang/String;[I)I",
                                                                                                                                                                 "(Ljava/lang/String;[Ljava/lang/String;)I"}),
                                                                                                              new ExactMethodMatcher("executeQuery",
                                                                                                                                            "(Ljava/lang/String;)Ljava/sql/ResultSet;")});

    public SqlStatementPointCut(ClassTransformer classTransformer) {
        this(ServiceFactory.getConfigService().getDefaultAgentConfig());
    }

    private SqlStatementPointCut(AgentConfig config) {
        super(new PointCutConfiguration("jdbc_statement"), getClassMatcher(config), METHOD_MATCHER);
    }

    private static ClassMatcher getClassMatcher(AgentConfig agentConfig) {
        Collection matchers = new ArrayList(2);
        if (agentConfig.isGenericJDBCSupportEnabled()) {
            matchers.add(new InterfaceMatcher("java/sql/Statement"));
        }
        if (agentConfig.getJDBCSupport().contains("mysql")) {
            matchers.add(new ExactClassMatcher("com/mysql/jdbc/Statement"));
        }

        return OrClassMatcher.getClassMatcher(matchers);
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object statement, Object[] args) {
        if (args.length > 0) {
            Tracer parent = transaction.getTransactionActivity().getLastTracer();
            if ((parent instanceof SqlStatementTracer)) {
                if (Agent.LOG.isLoggable(Level.FINEST)) {
                    String msg = MessageFormat
                                         .format("Skipping sql statement because last tracer is a SqlStatementTracer:"
                                                         + " {0}",
                                                        new Object[] {statement.getClass().getName()});

                    Agent.LOG.finest(msg);
                }
                return null;
            }
            DefaultStatementData stmtWrapper =
                    new DefaultStatementData(transaction.getDatabaseStatementParser(), (Statement) statement,
                                                    (String) args[0]);

            if (Agent.LOG.isLoggable(Level.FINEST)) {
                String msg = MessageFormat.format("Created SqlStatementTracer for: {0}",
                                                         new Object[] {statement.getClass().getName()});
                Agent.LOG.finest(msg);
            }
            return new SqlStatementTracer(transaction, sig, statement, stmtWrapper);
        }
        return null;
    }
}