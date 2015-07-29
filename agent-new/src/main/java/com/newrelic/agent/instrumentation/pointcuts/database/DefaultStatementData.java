package com.newrelic.agent.instrumentation.pointcuts.database;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.regex.Pattern;

import com.newrelic.agent.Agent;
import com.newrelic.agent.database.DatabaseStatementParser;
import com.newrelic.agent.database.ParsedDatabaseStatement;

public class DefaultStatementData implements StatementData {
    static final Pattern SPLIT_STATEMENT_PATTERN = Pattern.compile(";");
    private final Statement statement;
    private final DatabaseStatementParser databaseStatementParser;
    private final String sql;
    private volatile ParsedDatabaseStatement parsedDatabaseStatement;
    private volatile long timestamp;

    public DefaultStatementData(DatabaseStatementParser databaseStatementParser, Statement statement, String sql) {
        this(databaseStatementParser, statement, sql, null);
    }

    public DefaultStatementData(DatabaseStatementParser databaseStatementParser, Statement statement, String sql,
                                ParsedDatabaseStatement parsedStatement) {
        this.databaseStatementParser = databaseStatementParser;

        this.sql = sql;

        this.statement = statement;
        this.parsedDatabaseStatement = parsedStatement;
    }

    static String getFirstSqlStatement(String sql) {
        int index = sql.indexOf(';');
        if (index > 0) {
            return sql.substring(0, index);
        }
        return sql;
    }

    public String getSql() {
        return this.sql;
    }

    public Statement getStatement() {
        return this.statement;
    }

    public StatementData finalizeStatementData() {
        return this;
    }

    public ParsedDatabaseStatement getParsedStatement(Object returnValue, long configTimestamp) {
        if (this.parsedDatabaseStatement == null) {
            ResultSetMetaData metaData = null;
            try {
                if ((returnValue instanceof ResultSet)) {
                    metaData = ((ResultSet) returnValue).getMetaData();
                }
            } catch (Exception e) {
                if (Agent.isDebugEnabled()) {
                    Agent.LOG.log(Level.FINER, "Unable to get the result set meta data from a statement", e);
                }
            }
            this.timestamp = System.nanoTime();
            this.parsedDatabaseStatement = this.databaseStatementParser.getParsedDatabaseStatement(getSql(), metaData);
        } else if (configTimestamp > this.timestamp) {
            this.parsedDatabaseStatement = null;
            this.timestamp = 0L;
            return getParsedStatement(returnValue, configTimestamp);
        }
        return this.parsedDatabaseStatement;
    }
}