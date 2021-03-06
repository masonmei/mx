package com.newrelic.agent.instrumentation.pointcuts.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.newrelic.agent.database.RecordSql;
import com.newrelic.agent.tracers.ISqlStatementTracer;

class PreparedStatementExplainPlanExecutor extends DefaultExplainPlanExecutor {
    private final Object[] sqlParameters;

    public PreparedStatementExplainPlanExecutor(ISqlStatementTracer tracer, String originalSqlStatement,
                                                Object[] sqlParameters, RecordSql recordSql) {
        super(tracer, originalSqlStatement, recordSql);
        this.sqlParameters = sqlParameters;
    }

    protected Statement createStatement(Connection connection, String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    protected ResultSet executeStatement(Statement statement, String sql) throws SQLException {
        PreparedStatement preparedStatement = (PreparedStatement) statement;
        setSqlParameters(preparedStatement);
        return preparedStatement.executeQuery();
    }

    private void setSqlParameters(PreparedStatement preparedStatement) throws SQLException {
        if (null == sqlParameters) {
            return;
        }
        int length = sqlParameters.length;
        try {
            length = Math.min(length, preparedStatement.getMetaData().getColumnCount());
        } catch (Throwable t) {
        }
        try {
            for (int i = 0; i < length; i++) {
                preparedStatement.setObject(i + 1, sqlParameters[i]);
            }
        } catch (Throwable t) {
        }
    }
}