package com.newrelic.agent.async;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.SimpleStatsEngine;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.tracers.AbstractTracer;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.SkipTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.transaction.TransactionCounts;
import com.newrelic.agent.util.Strings;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class AsyncTracer extends AbstractTracer
{
  private static final int INITIAL_PARAMETER_MAP_SIZE = 5;
  private final long startTime;
  private final long duration;
  private Map<String, Object> attributes;
  private volatile Tracer parentTracer;
  private final ClassMethodSignature classMethodSignature;
  private MetricNameFormat metricNameFormat;
  private final boolean metricProducer;
  private boolean isParent;
  private final TransactionActivity tracerParentActivty;

  public AsyncTracer(TransactionActivity rootTxActivty, TransactionActivity tracerParentActivty, ClassMethodSignature sig, MetricNameFormat metricNameFormatter, long startTime, long endTime)
  {
    super(rootTxActivty);
    this.tracerParentActivty = tracerParentActivty;
    this.startTime = startTime;
    this.metricNameFormat = metricNameFormatter;
    this.classMethodSignature = sig;
    this.parentTracer = rootTxActivty.getLastTracer();
    this.metricProducer = true;
    this.duration = Math.max(0L, endTime - startTime);
  }

  public final void finish(Throwable throwable)
  {
    TransactionActivity activity = getTransaction().getTransactionActivity();
    try
    {
      activity.lockTracerStart();
      doFinish(throwable);
    } catch (Throwable t) {
      String msg = MessageFormat.format("An error occurred finishing tracer for class {0} : {1}", new Object[] { this.classMethodSignature.getClassName(), t });

      if (Agent.LOG.isLoggable(Level.FINER))
        Agent.LOG.log(Level.WARNING, msg, t);
      else
        Agent.LOG.warning(msg);
    }
    finally {
      activity.unlockTracerStart();
    }
    finish(191, null);

    if (Agent.isDebugEnabled())
      Agent.LOG.log(Level.FINE, "(Debug) Tracer.finish(Throwable)");
  }

  public void finish(int opcode, Object returnValue)
  {
    TransactionActivity activity = getTransactionActivity();
    try
    {
      activity.lockTracerStart();
      if (191 != opcode)
        doFinish(opcode, returnValue);
    }
    catch (Throwable t) {
      String msg = MessageFormat.format("An error occurred finishing tracer for class {0} : {1}", new Object[] { this.classMethodSignature.getClassName(), t.toString() });

      Agent.LOG.severe(msg);
      Agent.LOG.log(Level.FINER, msg, t);
    } finally {
      activity.unlockTracerStart();
    }

    if (this.parentTracer != null) {
      this.parentTracer.childTracerFinished(this);
    }
    try
    {
      recordMetrics(getTransaction().getTransactionActivity().getTransactionStats());
    } catch (Throwable t) {
      String msg = MessageFormat.format("An error occurred recording tracer metrics for class {0} : {1}", new Object[] { this.classMethodSignature.getClassName(), t.toString() });

      Agent.LOG.severe(msg);
      Agent.LOG.log(Level.FINER, msg, t);
    }
    try
    {
      if (!(this instanceof SkipTracer))
        activity.tracerFinished(this, opcode);
    }
    catch (Throwable t) {
      String msg = MessageFormat.format("An error occurred calling Transaction.tracerFinished() for class {0} : {1}", new Object[] { this.classMethodSignature.getClassName(), t.toString() });

      Agent.LOG.severe(msg);
      Agent.LOG.log(Level.FINER, msg, t);
    }
  }

  protected void doFinish(Throwable throwable)
  {
  }

  protected void doFinish(int opcode, Object returnValue)
  {
  }

  static int sizeof(Object value)
  {
    int size = 0;
    if (value == null)
      return 0;
    if ((value instanceof String))
      return ((String)value).length();
    if ((value instanceof StackTraceElement)) {
      StackTraceElement elem = (StackTraceElement)value;

      return sizeof(elem.getClassName()) + sizeof(elem.getFileName()) + sizeof(elem.getMethodName()) + 10;
    }if ((value instanceof Object[])) {
      for (Object obj : (Object[])value) {
        size += sizeof(obj);
      }
    }
    return size;
  }

  public void setAttribute(String key, Object value)
  {
    if (value.getClass().isArray()) {
      value = Arrays.asList((Object[])value);
    }

    getTransaction().getTransactionCounts().incrementSize(sizeof(value));

    if (this.attributes == null) {
      this.attributes = new HashMap(1, 5.0F);
    }
    this.attributes.put(key, value);
  }

  public Object getAttribute(String key)
  {
    return this.attributes == null ? null : this.attributes.get(key);
  }

  public Map<String, Object> getAttributes()
  {
    if (this.attributes == null) {
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(this.attributes);
  }

  public long getRunningDurationInNanos()
  {
    return this.duration;
  }

  public long getDurationInMilliseconds()
  {
    return TimeUnit.MILLISECONDS.convert(getDuration(), TimeUnit.NANOSECONDS);
  }

  public long getDuration()
  {
    return this.duration;
  }

  public long getExclusiveDuration()
  {
    return 0L;
  }

  public long getEndTime()
  {
    return this.startTime + this.duration;
  }

  public long getEndTimeInMilliseconds()
  {
    return TimeUnit.MILLISECONDS.convert(getEndTime(), TimeUnit.NANOSECONDS);
  }

  public long getStartTime()
  {
    return this.startTime;
  }

  public long getStartTimeInMilliseconds()
  {
    return TimeUnit.MILLISECONDS.convert(getStartTime(), TimeUnit.NANOSECONDS);
  }

  public Tracer getParentTracer()
  {
    return this.parentTracer;
  }

  public void setParentTracer(Tracer tracer)
  {
    this.parentTracer = tracer;
  }

  public TransactionActivity getTracerParentActivty() {
    return this.tracerParentActivty;
  }

  public String getRequestMetricName() {
    return null;
  }

  protected final void setMetricNameFormat(MetricNameFormat nameFormat) {
    this.metricNameFormat = nameFormat;
  }

  protected final MetricNameFormat getMetricNameFormat() {
    return this.metricNameFormat;
  }

  public final String getMetricName()
  {
    return this.metricNameFormat == null ? null : this.metricNameFormat.getMetricName();
  }

  public final String getTransactionSegmentName()
  {
    return this.metricNameFormat == null ? null : this.metricNameFormat.getTransactionSegmentName();
  }

  public final String getTransactionSegmentUri()
  {
    return this.metricNameFormat == null ? null : this.metricNameFormat.getTransactionSegmentUri();
  }

  protected void recordMetrics(TransactionStats transactionStats)
  {
    if (getTransaction().isIgnore()) {
      return;
    }
    if (isMetricProducer()) {
      String metricName = getMetricName();
      if (metricName != null)
      {
        ResponseTimeStats stats = transactionStats.getScopedStats().getResponseTimeStats(metricName);
        stats.recordResponseTimeInNanos(getDuration(), getExclusiveDuration());
      }

      if (getRollupMetricNames() != null) {
        for (String name : getRollupMetricNames()) {
          ResponseTimeStats stats = transactionStats.getUnscopedStats().getResponseTimeStats(name);
          stats.recordResponseTimeInNanos(getDuration(), getExclusiveDuration());
        }
      }
      if (getExclusiveRollupMetricNames() != null) {
        for (String name : getExclusiveRollupMetricNames()) {
          ResponseTimeStats stats = transactionStats.getUnscopedStats().getResponseTimeStats(name);
          stats.recordResponseTimeInNanos(getExclusiveDuration(), getExclusiveDuration());
        }
      }
      doRecordMetrics(transactionStats);
    }
  }

  protected void doRecordMetrics(TransactionStats transactionStats)
  {
  }

  public final boolean isParent()
  {
    return this.isParent;
  }

  public void childTracerFinished(Tracer child)
  {
    this.isParent = ((child.isMetricProducer()) && (child.isTransactionSegment()) && (!(child instanceof SkipTracer)));
  }

  public ClassMethodSignature getClassMethodSignature()
  {
    return this.classMethodSignature;
  }

  public boolean isTransactionSegment()
  {
    return true;
  }

  public boolean isMetricProducer()
  {
    return this.metricProducer;
  }

  public TransactionSegment getTransactionSegment(TransactionTracerConfig ttConfig, SqlObfuscator sqlObfuscator, long startTime, TransactionSegment lastSibling)
  {
    return new TransactionSegment(ttConfig, sqlObfuscator, startTime, this);
  }

  public void setMetricName(String[] metricNameParts)
  {
    String metricName = Strings.join('/', metricNameParts);
    setMetricNameFormat(new SimpleMetricNameFormat(metricName));
  }

  public void setMetricNameFormatInfo(String metricName, String transactionSegmentName, String transactionSegmentUri)
  {
  }
}