package com.newrelic.agent.trace;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.JSONStreamAware;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ISqlStatementTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.util.StackTraces;
import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransactionSegment
  implements JSONStreamAware
{
  private static final String PARTIAL_TRACE = "partialtrace";
  private static final Pattern INSERT_INTO_VALUES_STATEMENT = Pattern.compile("\\s*insert\\s+into\\s+([^\\s(,]*)\\s+values.*", 2);
  private static final String URI_PARAM_NAME = "uri";
  private String metricName;
  private final List<TransactionSegment> children;
  private final long entryTimestamp;
  private long exitTimestamp;
  private final Map<String, Object> tracerAttributes;
  private int callCount = 1;
  private final String uri;
  private final SqlObfuscator sqlObfuscator;
  private final TransactionTracerConfig ttConfig;
  private final List<StackTraceElement> parentStackTrace;
  private final ClassMethodSignature classMethodSignature;

  public TransactionSegment(TransactionTracerConfig ttConfig, SqlObfuscator sqlObfuscator, long startTime, Tracer tracer)
  {
    this(ttConfig, sqlObfuscator, startTime, tracer, null);
  }

  TransactionSegment(TransactionTracerConfig ttConfig, SqlObfuscator sqlObfuscator, long startTime, Tracer tracer, TransactionSegment childSegment)
  {
    this.ttConfig = ttConfig;
    this.sqlObfuscator = sqlObfuscator;
    this.metricName = getMetricName(tracer);
    this.uri = getUri(tracer);
    if (childSegment == null) {
      this.children = Lists.newArrayList();
    } else {
      this.children = new ArrayList(1);
      this.children.add(childSegment);
    }
    this.entryTimestamp = (tracer.getStartTimeInMilliseconds() - startTime);
    this.exitTimestamp = (tracer.getEndTimeInMilliseconds() - startTime);
    this.tracerAttributes = getTracerAttributes(tracer);
    this.classMethodSignature = tracer.getClassMethodSignature();

    this.parentStackTrace = getParentStackTrace(tracer);
  }

  private List<StackTraceElement> getParentStackTrace(Tracer tracer) {
    if (tracer.getParentTracer() != null) {
      return (List)tracer.getParentTracer().getAttribute("backtrace");
    }

    return null;
  }

  private Map<String, Object> getTracerAttributes(Tracer tracer) {
    if ((tracer instanceof ISqlStatementTracer)) {
      Object sql = ((ISqlStatementTracer)tracer).getSql();
      if (sql != null) {
        tracer.setAttribute("sql", sql);
      }
    }
    return tracer.getAttributes();
  }

  private static String getMetricName(Tracer tracer) {
    String metricName = tracer.getTransactionSegmentName();
    if ((metricName == null) || (metricName.trim().length() == 0)) {
      if (Agent.isDebugEnabled()) {
        throw new RuntimeException(MessageFormat.format("Encountered a transaction segment with an invalid metric name. {0}", new Object[] { tracer.getClass().getName() }));
      }

      metricName = tracer.getClass().getName() + "*";
    }

    return metricName;
  }

  public Map<String, Object> getTraceParameters() {
    return Collections.unmodifiableMap(this.tracerAttributes);
  }

  private static String getUri(Tracer tracer) {
    return tracer.getTransactionSegmentUri();
  }

  void setMetricName(String name) {
    this.metricName = name;
  }

  public Collection<TransactionSegment> getChildren() {
    return Collections.unmodifiableCollection(this.children);
  }

  public String getMetricName()
  {
    return this.metricName;
  }

  public void addChild(TransactionSegment sample) {
    try {
      this.children.add(sample);
    } catch (UnsupportedOperationException e) {
      String msg = MessageFormat.format("Unable to add transaction segment {0} to parent segment {1}", new Object[] { sample, this });

      Agent.LOG.info(msg);
    }
  }

  public String toString()
  {
    return this.metricName;
  }

  public void writeJSONString(Writer writer) throws IOException
  {
    Map params = new HashMap(this.tracerAttributes);
    processStackTraces(params);
    processSqlParams(params);

    if (this.callCount > 1) {
      params.put("call_count", Integer.valueOf(this.callCount));
    }

    if ((this.uri != null) && (this.uri.length() > 0)) {
      params.put("uri", this.uri);
    }

    JSONArray.writeJSONString(Arrays.asList(new Object[] { Long.valueOf(this.entryTimestamp), Long.valueOf(this.exitTimestamp), this.metricName, params, this.children, this.classMethodSignature.getClassName(), this.classMethodSignature.getMethodName() }), writer);
  }

  private void processSqlParams(Map<String, Object> params)
  {
    Object sqlObj = params.remove("sql");
    if (sqlObj == null) {
      return;
    }
    String sql = this.sqlObfuscator.obfuscateSql(sqlObj.toString());
    if (sql == null) {
      return;
    }
    if (INSERT_INTO_VALUES_STATEMENT.matcher(sql).matches()) {
      int maxLength = this.ttConfig.getInsertSqlMaxLength();
      sql = truncateSql(sql, maxLength);
    }
    if (this.ttConfig.isLogSql()) {
      Agent.LOG.log(Level.INFO, MessageFormat.format("{0} SQL: {1}", new Object[] { this.ttConfig.getRecordSql(), sql }));
      return;
    }
    params.put(this.sqlObfuscator.isObfuscating() ? "sql_obfuscated" : "sql", sql);
  }

  private void processStackTraces(Map<String, Object> params)
  {
    List backtrace = (List)params.remove("backtrace");
    if (backtrace != null) {
      List preStackTraces = StackTraces.scrubAndTruncate(backtrace);
      List postParentRemovalTrace = StackTraces.toStringListRemoveParent(preStackTraces, this.parentStackTrace);

      if (preStackTraces.size() == postParentRemovalTrace.size())
      {
        params.put("backtrace", postParentRemovalTrace);
      }
      else
        params.put("partialtrace", postParentRemovalTrace);
    }
  }

  public void merge(Tracer tracer)
  {
    this.callCount += 1;
    this.exitTimestamp += tracer.getDurationInMilliseconds();
  }

  public static String truncateSql(String sql, int maxLength) {
    int len = sql.length();
    if (len > maxLength) {
      return MessageFormat.format("{0}..({1} more chars)", new Object[] { sql.substring(0, maxLength), Integer.valueOf(len - maxLength) });
    }
    return sql;
  }
}