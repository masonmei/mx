package com.newrelic.agent.database;

import java.sql.ResultSetMetaData;

public abstract interface DatabaseStatementParser {
    public static final String SELECT_OPERATION = "select";
    public static final String INSERT_OPERATION = "insert";
    public static final ParsedDatabaseStatement UNPARSEABLE_STATEMENT =
            new ParsedDatabaseStatement(null, "other", true);

    public abstract ParsedDatabaseStatement getParsedDatabaseStatement(String paramString,
                                                                       ResultSetMetaData paramResultSetMetaData);
}