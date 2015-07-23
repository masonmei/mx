package com.newrelic.agent.instrumentation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.ClassTransformerConfig;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.NoMatchMatcher;
import com.newrelic.agent.instrumentation.pointcuts.database.PreparedStatementExtension;
import com.newrelic.agent.service.ServiceFactory;

public class JDBCClassTransformer extends AbstractImplementationClassTransformer {
    public static final String DERBY_PREPARED_STATEMENT = "org/apache/derby/impl/jdbc/EmbedPreparedStatement";
    private static final List<String> DEFAULT_JDBC_STATEMENT_CLASSES =
            Arrays.asList(new String[] {"org/apache/derby/impl/jdbc/EmbedPreparedStatement",
                                               "com/mysql/jdbc/PreparedStatement",
                                               "com/microsoft/sqlserver/jdbc/SQLServerPreparedStatement",
                                               "net/sourceforge/jtds/jdbc/JtdsPreparedStatement",
                                               "oracle/jdbc/driver/OraclePreparedStatementWrapper",
                                               "org/postgresql/jdbc2/AbstractJdbc2Statement",
                                               "oracle/jdbc/driver/OraclePreparedStatement"});
    private boolean genericJdbcSupportEnabled;

    public JDBCClassTransformer(ClassTransformer classTransformer) {
        super(classTransformer, true, PreparedStatementExtension.class, getJdbcStatementClassMatcher(),
                     NoMatchMatcher.MATCHER, "java/sql/PreparedStatement");

        genericJdbcSupportEnabled = true;
    }

    private static ClassMatcher getJdbcStatementClassMatcher() {
        StringBuilder sb = new StringBuilder();
        sb.append("JDBC statement classes: ");
        Set<String> jdbcClasses = getJdbcStatementClasses();
        for (String jdbcClass : jdbcClasses) {
            sb.append("\n").append(jdbcClass);
        }
        Agent.LOG.fine(sb.toString());
        return ExactClassMatcher.or(jdbcClasses.toArray(new String[0]));
    }

    public static Set<String> getJdbcStatementClasses() {
        Set result = new HashSet();
        result.addAll(DEFAULT_JDBC_STATEMENT_CLASSES);
        result.addAll(getJdbcStatementClassesInConfig());
        return Collections.unmodifiableSet(result);
    }

    private static List<String> getJdbcStatementClassesInConfig() {
        List result = new LinkedList();
        ClassTransformerConfig config =
                ServiceFactory.getConfigService().getDefaultAgentConfig().getClassTransformerConfig();
        for (String configClass : config.getJdbcStatements()) {
            result.add(configClass);
        }
        return result;
    }

    protected ClassVisitor createClassVisitor(ClassReader cr, ClassWriter cw, String className, ClassLoader loader) {
        ClassVisitor adapter = new AddInterfaceAdapter(cw, className, PreparedStatementExtension.class);
        adapter = RequireMethodsAdapter
                          .getRequireMethodsAdaptor(adapter, className, PreparedStatementExtension.class, loader);

        adapter = new FieldAccessorGeneratingClassAdapter(adapter, className, PreparedStatementExtension.class);
        return adapter;
    }

    protected boolean isGenericInterfaceSupportEnabled() {
        return genericJdbcSupportEnabled;
    }
}