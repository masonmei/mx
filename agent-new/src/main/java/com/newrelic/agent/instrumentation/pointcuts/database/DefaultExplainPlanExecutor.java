package com.newrelic.agent.instrumentation.pointcuts.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.database.DatabaseVendor;
import com.newrelic.agent.database.RecordSql;
import com.newrelic.agent.tracers.ISqlStatementTracer;

public class DefaultExplainPlanExecutor implements ExplainPlanExecutor {
    private final String originalSqlStatement;
    private final RecordSql recordSql;
    private ISqlStatementTracer tracer;

    public DefaultExplainPlanExecutor(ISqlStatementTracer tracer, String originalSqlStatement, RecordSql recordSql) {
        this.originalSqlStatement = originalSqlStatement;
        this.tracer = tracer;
        this.recordSql = recordSql;
    }

    private Object[] getExplainPlanFromResultSet(DatabaseVendor vendor, ResultSet rs, RecordSql recordSql)
            throws SQLException {
        int columnCount = rs.getMetaData().getColumnCount();
        if (columnCount > 0) {
            Collection explains = vendor.parseExplainPlanResultSet(columnCount, rs, recordSql);
            return new Object[] {explains};
        }
        return null;
    }

    public void runExplainPlan(DatabaseService databaseService, Connection connection, DatabaseVendor vendor)
            throws SQLException {
        String sql = this.originalSqlStatement;
        try {
            sql = vendor.getExplainPlanSql(sql);
        } catch (SQLException e) {
            this.tracer.setExplainPlan(new Object[] {e.getMessage()});
            return;
        }
        Agent.LOG.finer("Running explain: " + sql);
        ResultSet resultSet = null;
        Statement statement = null;
        Object[] explainPlan = null;
        try {
            statement = createStatement(connection, sql);
            resultSet = executeStatement(statement, sql);
            explainPlan = getExplainPlanFromResultSet(vendor, resultSet, this.recordSql);
        } catch (Exception e) {
            explainPlan = new Object[] {"An error occurred running explain plan : " + e.getMessage()};
            Agent.LOG.log(Level.FINER, "explain plan error", e);
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (Exception e) {
                    Agent.LOG.log(Level.FINER, "Unable to close result set", e);
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (Exception e) {
                    Agent.LOG.log(Level.FINER, "Unable to close statement", e);
                }
            }
        }
        if (explainPlan != null) {
            this.tracer.setExplainPlan(explainPlan);
        }
    }

    protected ResultSet executeStatement(Statement statement, String sql) throws SQLException {
        return statement.executeQuery(sql);
    }

    protected Statement createStatement(Connection connection, String sql) throws SQLException {
        return connection.createStatement();
    }
}