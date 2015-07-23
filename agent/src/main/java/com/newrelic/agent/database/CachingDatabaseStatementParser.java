package com.newrelic.agent.database;

import java.sql.ResultSetMetaData;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.newrelic.agent.Agent;

public class CachingDatabaseStatementParser implements DatabaseStatementParser {
    private final DatabaseStatementParser databaseStatementParser;
    private volatile Cache<String, ParsedDatabaseStatement> statements;

    public CachingDatabaseStatementParser(DatabaseStatementParser databaseStatementParser) {
        this.databaseStatementParser = databaseStatementParser;
    }

    private Cache<String, ParsedDatabaseStatement> getOrCreateCache() {
        if (null == statements) {
            synchronized(this) {
                if (null == statements) {
                    statements = CacheBuilder.newBuilder().maximumSize(100L).build();
                }
            }
        }
        return statements;
    }

    public ParsedDatabaseStatement getParsedDatabaseStatement(final String statement,
                                                              final ResultSetMetaData resultSetMetaData) {
        Throwable toLog = null;
        try {
            return (ParsedDatabaseStatement) getOrCreateCache().get(statement, new Callable() {
                public ParsedDatabaseStatement call() throws Exception {
                    return databaseStatementParser.getParsedDatabaseStatement(statement, resultSetMetaData);
                }
            });
        } catch (ExecutionException ee) {
            toLog = ee;
            if (ee.getCause() != null) {
                toLog = ee.getCause();
            }
        } catch (Exception ex) {
            toLog = ex;
        }
        Agent.LOG.log(Level.FINEST, "In cache.get() or its loader:", toLog);
        return UNPARSEABLE_STATEMENT;
    }
}