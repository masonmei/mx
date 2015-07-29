package com.newrelic.agent.database;

public enum RecordSql {
    obfuscated, raw, off;

    public static RecordSql get(String value) {
        return (RecordSql) Enum.valueOf(RecordSql.class, value);
    }
}