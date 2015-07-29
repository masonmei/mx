package com.newrelic.agent.sql;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.instrumentation.pointcuts.database.SqlStatementTracer;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.util.StackTraces;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class SqlStatementInfo
  implements Comparable<SqlStatementInfo>
{
  private final AtomicReference<SqlTracerInfo> slowestSql = new AtomicReference();
  private final int id;
  private final AtomicInteger callCount = new AtomicInteger();
  private final AtomicLong total = new AtomicLong();
  final AtomicLong max = new AtomicLong();
  private final AtomicLong min = new AtomicLong(9223372036854775807L);
  private String cachedSql = null;

  SqlStatementInfo(TransactionData td, SqlStatementTracer sqlTracer, int id) {
    this.slowestSql.set(new SqlTracerInfo(td, sqlTracer));
    this.id = id;
  }

  public TransactionData getTransactionData() {
    return ((SqlTracerInfo)this.slowestSql.get()).getTransactionData();
  }

  public SqlStatementTracer getSqlStatementTracer() {
    return ((SqlTracerInfo)this.slowestSql.get()).getSqlTracer();
  }

  public int compareTo(SqlStatementInfo other)
  {
    Long thisMax = Long.valueOf(this.max.get());
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
    this.callCount.incrementAndGet();
    long duration = sqlTracer.getDuration();
    this.total.addAndGet(duration);
    replaceMin(duration);
    replaceMax(duration);
    replaceSqlTracer(td, sqlTracer);
  }

  public void aggregate(SqlStatementInfo other) {
    long duration = other.getSqlStatementTracer().getDuration();
    this.total.addAndGet(other.getTotalInNano());
    this.callCount.addAndGet(other.getCallCount());
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
    return this.id;
  }

  public String getSql() {
    if (this.cachedSql != null) {
      return this.cachedSql;
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
    this.cachedSql = TransactionSegment.truncateSql(obfuscatedSql == null ? sql : obfuscatedSql, maxSqlLength);
    return this.cachedSql;
  }

  public String getRequestUri() {
    return getTransactionData().getRequestUri();
  }

  public int getCallCount() {
    return this.callCount.get();
  }

  public long getTotalInNano() {
    return this.total.get();
  }

  public long getTotalInMillis() {
    return TimeUnit.MILLISECONDS.convert(this.total.get(), TimeUnit.NANOSECONDS);
  }

  public long getMinInMillis() {
    return TimeUnit.MILLISECONDS.convert(this.min.get(), TimeUnit.NANOSECONDS);
  }

  public long getMaxInMillis() {
    return TimeUnit.MILLISECONDS.convert(this.max.get(), TimeUnit.NANOSECONDS);
  }

  public Map<String, Object> getParameters() {
    SqlStatementTracer sqlTracer = getSqlStatementTracer();
    return createParameters(sqlTracer);
  }

  private Map<String, Object> createParameters(SqlStatementTracer sqlTracer)
  {
    Map parameters = new HashMap();
    Object explainPlan = sqlTracer.getAttribute("explanation");
    parameters.put("explain_plan", explainPlan);
    List backtrace = (List)sqlTracer.getAttribute("backtrace");
    if (backtrace != null) {
      backtrace = StackTraces.scrubAndTruncate(backtrace);
      List backtraceStrings = StackTraces.toStringList(backtrace);
      parameters.put("backtrace", backtraceStrings);
    }
    return parameters;
  }

  private void replaceMin(long duration) {
    while (true) {
      long currentDuration = this.min.get();
      if (duration >= currentDuration) {
        return;
      }
      if (this.min.compareAndSet(currentDuration, duration))
        return;
    }
  }

  private void replaceMax(long duration)
  {
    while (true) {
      long currentDuration = this.max.get();
      if (duration <= currentDuration) {
        return;
      }
      if (this.max.compareAndSet(currentDuration, duration))
        return;
    }
  }

  private void replaceSqlTracer(TransactionData td, SqlStatementTracer sqlTracer)
  {
    while (true) {
      SqlTracerInfo current = (SqlTracerInfo)this.slowestSql.get();
      if (sqlTracer.getDuration() <= current.getSqlTracer().getDuration()) {
        return;
      }
      SqlTracerInfo update = new SqlTracerInfo(td, sqlTracer);
      if (this.slowestSql.compareAndSet(current, update))
        return;
    }
  }

  public void setTransactionData(TransactionData td)
  {
    ((SqlTracerInfo)this.slowestSql.get()).setTransactionData(td);
  }
}