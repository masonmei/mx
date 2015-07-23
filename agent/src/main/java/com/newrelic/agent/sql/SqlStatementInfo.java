package com.newrelic.agent.sql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.instrumentation.pointcuts.database.SqlStatementTracer;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.util.StackTraces;

class SqlStatementInfo implements Comparable<SqlStatementInfo> {
    final AtomicLong max = new AtomicLong();
    private final AtomicReference<SqlTracerInfo> slowestSql = new AtomicReference();
    private final int id;
    private final AtomicInteger callCount = new AtomicInteger();
    private final AtomicLong total = new AtomicLong();
    private final AtomicLong min = new AtomicLong(9223372036854775807L);
    private String cachedSql = null;

    SqlStatementInfo(TransactionData td, SqlStatementTracer sqlTracer, int id) {
        slowestSql.set(new SqlTracerInfo(td, sqlTracer));
        this.id = id;
    }

    public TransactionData getTransactionData() {
        return ((SqlTracerInfo) slowestSql.get()).getTransactionData();
    }

    public void setTransactionData(TransactionData td) {
        ((SqlTracerInfo) slowestSql.get()).setTransactionData(td);
    }

    public SqlStatementTracer getSqlStatementTracer() {
        return ((SqlTracerInfo) slowestSql.get()).getSqlTracer();
    }

    public int compareTo(SqlStatementInfo other) {
        Long thisMax = Long.valueOf(max.get());
        Long otherMax = Long.valueOf(other.max.get());
        int compare = thisMax.compareTo(otherMax);
        if (compare == 0) {
            return getSql().compareTo(other.getSql());
        }
        return compare;
    }

    public void aggregate(SqlStatementTracer sqlTracer) {
        aggregate(null, sqlTracer);
    }

    public void aggregate(TransactionData td, SqlStatementTracer sqlTracer) {
        callCount.incrementAndGet();
        long duration = sqlTracer.getDuration();
        total.addAndGet(duration);
        replaceMin(duration);
        replaceMax(duration);
        replaceSqlTracer(td, sqlTracer);
    }

    public void aggregate(SqlStatementInfo other) {
        long duration = other.getSqlStatementTracer().getDuration();
        total.addAndGet(other.getTotalInNano());
        callCount.addAndGet(other.getCallCount());
        replaceMin(duration);
        replaceMax(duration);
        replaceSqlTracer(other.getTransactionData(), other.getSqlStatementTracer());
    }

    public SqlTrace asSqlTrace() {
        SqlStatementTracer sqlTracer = getSqlStatementTracer();
        DatabaseService dbService = ServiceFactory.getDatabaseService();
        dbService.runExplainPlan(sqlTracer);
        return new SqlTraceImpl(this);
    }

    public String getBlameMetricName() {
        return getTransactionData().getBlameMetricName();
    }

    public String getMetricName() {
        return getSqlStatementTracer().getMetricName();
    }

    public int getId() {
        return id;
    }

    public String getSql() {
        if (cachedSql != null) {
            return cachedSql;
        }
        SqlStatementTracer sqlTracer = getSqlStatementTracer();
        String sql = sqlTracer.getSql().toString();

        String obfuscatedSql = null;
        if (getTransactionData() != null) {
            String appName = getTransactionData().getApplicationName();
            SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getSqlObfuscator(appName);
            obfuscatedSql = sqlObfuscator.obfuscateSql(sql);
        }

        int maxSqlLength = sqlTracer.getTransaction().getTransactionTracerConfig().getInsertSqlMaxLength();
        cachedSql = TransactionSegment.truncateSql(obfuscatedSql == null ? sql : obfuscatedSql, maxSqlLength);
        return cachedSql;
    }

    public String getRequestUri() {
        return getTransactionData().getRequestUri();
    }

    public int getCallCount() {
        return callCount.get();
    }

    public long getTotalInNano() {
        return total.get();
    }

    public long getTotalInMillis() {
        return TimeUnit.MILLISECONDS.convert(total.get(), TimeUnit.NANOSECONDS);
    }

    public long getMinInMillis() {
        return TimeUnit.MILLISECONDS.convert(min.get(), TimeUnit.NANOSECONDS);
    }

    public long getMaxInMillis() {
        return TimeUnit.MILLISECONDS.convert(max.get(), TimeUnit.NANOSECONDS);
    }

    public Map<String, Object> getParameters() {
        SqlStatementTracer sqlTracer = getSqlStatementTracer();
        return createParameters(sqlTracer);
    }

    private Map<String, Object> createParameters(SqlStatementTracer sqlTracer) {
        Map parameters = new HashMap();
        Object explainPlan = sqlTracer.getAttribute("explanation");
        parameters.put("explain_plan", explainPlan);
        List backtrace = (List) sqlTracer.getAttribute("backtrace");
        if (backtrace != null) {
            backtrace = StackTraces.scrubAndTruncate(backtrace);
            List backtraceStrings = StackTraces.toStringList(backtrace);
            parameters.put("backtrace", backtraceStrings);
        }
        return parameters;
    }

    private void replaceMin(long duration) {
        while (true) {
            long currentDuration = min.get();
            if (duration >= currentDuration) {
                return;
            }
            if (min.compareAndSet(currentDuration, duration)) {
                return;
            }
        }
    }

    private void replaceMax(long duration) {
        while (true) {
            long currentDuration = max.get();
            if (duration <= currentDuration) {
                return;
            }
            if (max.compareAndSet(currentDuration, duration)) {
                return;
            }
        }
    }

    private void replaceSqlTracer(TransactionData td, SqlStatementTracer sqlTracer) {
        while (true) {
            SqlTracerInfo current = (SqlTracerInfo) slowestSql.get();
            if (sqlTracer.getDuration() <= current.getSqlTracer().getDuration()) {
                return;
            }
            SqlTracerInfo update = new SqlTracerInfo(td, sqlTracer);
            if (slowestSql.compareAndSet(current, update)) {
                return;
            }
        }
    }
}