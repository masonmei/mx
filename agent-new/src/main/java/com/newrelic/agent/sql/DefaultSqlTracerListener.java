package com.newrelic.agent.sql;

import java.util.Collections;
import java.util.List;

import com.newrelic.agent.instrumentation.pointcuts.database.SqlStatementTracer;

public class DefaultSqlTracerListener implements SqlTracerListener {
    private static final int MAX_SQL_TRACERS = 175;
    private final Object lock = new Object();
    private final double thresholdInMillis;
    private volatile BoundedConcurrentCache<String, SqlStatementInfo> sqlInfoCache = null;

    public DefaultSqlTracerListener(double thresholdInMillis) {
        this.thresholdInMillis = thresholdInMillis;
    }

    public void noticeSqlTracer(SqlStatementTracer sqlTracer) {
        if (sqlTracer.getDurationInMilliseconds() > this.thresholdInMillis) {
            synchronized(this.lock) {
                if (this.sqlInfoCache == null) {
                    this.sqlInfoCache = new BoundedConcurrentCache(175);
                }

                Object sqlObject = sqlTracer.getSql();
                if (sqlObject == null) {
                    return;
                }

                String sql = sqlObject.toString();
                String obfuscatedSql = ObfuscatorUtil.obfuscateSql(sql);
                if (obfuscatedSql == null) {
                    return;
                }

                SqlStatementInfo existingInfo = (SqlStatementInfo) this.sqlInfoCache.get(obfuscatedSql);
                if (existingInfo != null) {
                    existingInfo.aggregate(sqlTracer);
                    this.sqlInfoCache.putReplace(obfuscatedSql, existingInfo);
                } else {
                    SqlStatementInfo sqlInfo = new SqlStatementInfo(null, sqlTracer, obfuscatedSql.hashCode());
                    sqlInfo.aggregate(sqlTracer);
                    this.sqlInfoCache.putIfAbsent(obfuscatedSql, sqlInfo);
                }
            }
        }
    }

    public List<SqlStatementInfo> getSqlStatementInfo() {
        if (this.sqlInfoCache == null) {
            return Collections.emptyList();
        }
        return this.sqlInfoCache.asList();
    }
}