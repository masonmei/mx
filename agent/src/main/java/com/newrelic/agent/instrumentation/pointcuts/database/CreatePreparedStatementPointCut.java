package com.newrelic.agent.instrumentation.pointcuts.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OrClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import com.newrelic.agent.tracers.TracerFactory;

@com.newrelic.agent.instrumentation.pointcuts.PointCut
public class CreatePreparedStatementPointCut extends com.newrelic.agent.instrumentation.PointCut {
    public static final String CONNECTION_INTERFACE = "java/sql/Connection";
    static final MethodMatcher METHOD_MATCHER = OrMethodMatcher
                                                        .getMethodMatcher(new MethodMatcher[] {new ExactMethodMatcher
                                                                                                       ("prepareStatement",
                                                                                                                             new String[] {"(Ljava/lang/String;)Ljava/sql/PreparedStatement;",
                                                                                                                                                  "(Ljava/lang/String;III)Ljava/sql/PreparedStatement;",
                                                                                                                                                  "(Ljava/lang/String;II)Ljava/sql/PreparedStatement;",
                                                                                                                                                  "(Ljava/lang/String;I)Ljava/sql/PreparedStatement;",
                                                                                                                                                  "(Ljava/lang/String;[I)Ljava/sql/PreparedStatement;",
                                                                                                                                                  "(Ljava/lang/String;[Ljava/lang/String;)Ljava/sql/PreparedStatement;"}),
                                                                                                      new ExactMethodMatcher("prepareCall",
                                                                                                                                    new String[] {"(Ljava/lang/String;)Ljava/sql/CallableStatement;",
                                                                                                                                                         "(Ljava/lang/String;II)Ljava/sql/CallableStatement;",
                                                                                                                                                         "(Ljava/lang/String;III)Ljava/sql/CallableStatement;"})});
    private static final String MYSQL_CONNECTION_CLASS = "com/mysql/jdbc/Connection";
    private final TracerFactory tracerFactory;

    public CreatePreparedStatementPointCut(ClassTransformer classTransformer) {
        this(ServiceFactory.getConfigService().getDefaultAgentConfig());
    }

    private CreatePreparedStatementPointCut(AgentConfig config) {
        super(new PointCutConfiguration("jdbc_prepare_statement", null, isEnabledByDefault()), getClassMatcher(config),
                     METHOD_MATCHER);

        tracerFactory = new CreatePreparedStatementTracerFactory();
    }

    protected static boolean isEnabledByDefault() {
        Set jdbcSupport = ServiceFactory.getConfigService().getDefaultAgentConfig().getJDBCSupport();
        return (jdbcSupport.size() != 1) || (!jdbcSupport.contains("mysql"));
    }

    private static ClassMatcher getClassMatcher(AgentConfig agentConfig) {
        Collection matchers = new ArrayList(2);
        if (agentConfig.isGenericJDBCSupportEnabled()) {
            matchers.add(new InterfaceMatcher("java/sql/Connection"));
        }

        matchers.add(new ExactClassMatcher("com/mysql/jdbc/Connection"));

        matchers.add(new ExactClassMatcher("oracle/jdbc/driver/PhysicalConnection"));
        matchers.add(new ExactClassMatcher("oracle/jdbc/OracleConnectionWrapper"));

        return OrClassMatcher.getClassMatcher((ClassMatcher[]) matchers.toArray(new ClassMatcher[0]));
    }

    protected boolean isDispatcher() {
        return true;
    }

    protected PointCutInvocationHandler getPointCutInvocationHandlerImpl() {
        return tracerFactory;
    }
}