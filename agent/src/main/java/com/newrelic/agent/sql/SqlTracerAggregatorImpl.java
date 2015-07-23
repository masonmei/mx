package com.newrelic.agent.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.newrelic.agent.Agent;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.instrumentation.pointcuts.database.SqlStatementTracer;

public class SqlTracerAggregatorImpl implements SqlTracerAggregator {
    public static final String BACKTRACE_KEY = "backtrace";
    public static final String EXPLAIN_PLAN_KEY = "explain_plan";
    public static final int SQL_LIMIT_PER_REPORTING_PERIOD = 10;
    static final int MAX_SQL_STATEMENTS = 200;
    private final BoundedConcurrentCache<String, SqlStatementInfo> sqlStatements = new BoundedConcurrentCache(200);
    private final Lock readLock;
    private final Lock writeLock;

    public SqlTracerAggregatorImpl() {
        ReadWriteLock lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();
    }

    public List<SqlTrace> getAndClearSqlTracers() {
        List infos = null;
        infos = getAndClearSqlTracersUnderLock();

        if ((infos == null) || (infos.isEmpty())) {
            return Collections.emptyList();
        }
        return createSqlTraces(infos);
    }

    public int getSqlInfoCount() {
        return sqlStatements.size();
    }

    private List<SqlTrace> createSqlTraces(List<SqlStatementInfo> infos) {
        List<SqlStatementInfo> topInfos = getTopTracers(infos);
        List result = new ArrayList(topInfos.size());
        for (SqlStatementInfo topInfo : topInfos) {
            result.add(topInfo.asSqlTrace());
        }
        return result;
    }

    private List<SqlStatementInfo> getTopTracers(List<SqlStatementInfo> infos) {
        if (infos.size() <= 10) {
            return infos;
        }
        Collections.sort(infos);
        return infos.subList(infos.size() - 10, infos.size());
    }

    private List<SqlStatementInfo> getAndClearSqlTracersUnderLock() {
        writeLock.lock();
        try {
            List result = sqlStatements.asList();
            sqlStatements.clear();
            return result;
        } finally {
            writeLock.unlock();
        }
    }

    public void addSqlTracers(TransactionData td) {
        SqlTracerListener listener = td.getSqlTracerListener();
        if (listener == null) {
            Agent.LOG.finest("SqlTracerAggrator: addSqlTracers: no listener");
            return;
        }
        List sqlInfos = listener.getSqlStatementInfo();
        if (sqlInfos.isEmpty()) {
            Agent.LOG.finest("SqlTracerAggrator: addSqlTracers: no sql statement infos");
            return;
        }

        addSqlTracersUnderLock(td, sqlInfos);
    }

    private void addSqlTracersUnderLock(TransactionData td, List<SqlStatementInfo> sqlInfos) {
        readLock.lock();
        try {
            for (SqlStatementInfo sqlInfo : sqlInfos) {
                addSqlTracer(td, sqlInfo);
            }
        } finally {
            readLock.unlock();
        }
    }

    private void addSqlTracer(TransactionData td, SqlStatementInfo sqlInfo) {
        SqlStatementTracer sqlTracer = sqlInfo.getSqlStatementTracer();
        Object sqlObj = sqlTracer.getSql();
        String sql = sqlObj == null ? null : sqlObj.toString();
        if ((sql == null) || (sql.length() == 0)) {
            return;
        }
        String obfuscatedSql = ObfuscatorUtil.obfuscateSql(sql);
        if (obfuscatedSql == null) {
            return;
        }
        SqlStatementInfo existingInfo = (SqlStatementInfo) sqlStatements.get(obfuscatedSql);
        if (existingInfo != null) {
            existingInfo.aggregate(sqlInfo);
            sqlStatements.putReplace(obfuscatedSql, existingInfo);
        } else {
            if (sqlInfo.getTransactionData() == null) {
                sqlInfo.setTransactionData(td);
            }
            sqlStatements.putReplace(obfuscatedSql, sqlInfo);
        }
    }
}