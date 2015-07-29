package com.newrelic.agent.database;

import java.sql.ResultSetMetaData;

public interface DatabaseStatementParser {
    String SELECT_OPERATION = "select";
    String INSERT_OPERATION = "insert";
    ParsedDatabaseStatement UNPARSEABLE_STATEMENT = new ParsedDatabaseStatement(null, "other", true);

    ParsedDatabaseStatement getParsedDatabaseStatement(String paramString, ResultSetMetaData paramResultSetMetaData);
}