package com.newrelic.agent.tracers;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.TransactionState;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.SimpleStatsEngine;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionSegment;
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

public class DefaultTracer extends AbstractTracer
{
  public static final MetricNameFormat NULL_METRIC_NAME_FORMATTER = new SimpleMetricNameFormat(null);
  public static final String BACKTRACE_PARAMETER_NAME = "backtrace";
  protected static final int DEFAULT_TRACER_FLAGS = 6;
  private static final int INITIAL_PARAMETER_MAP_SIZE = 5;
  private final long startTime;
  private long duration;
  private long exclusiveDuration;
  private Map<String, Object> attributes;
  private Tracer parentTracer;
  private final ClassMethodSignature classMethodSignature;
  private Object invocationTarget;
  private MetricNameFormat metricNameFormat;
  private boolean isParent;
  private boolean childHasStackTrace;
  private final byte tracerFlags;

  public DefaultTracer(Transaction transaction, ClassMethodSignature sig, Object object, MetricNameFormat metricNameFormatter, int tracerFlags)
  {
    this(transaction.getTransactionActivity(), sig, object, metricNameFormatter, tracerFlags);
  }

  public DefaultTracer(TransactionActivity txa, ClassMethodSignature sig, Object object, MetricNameFormat metricNameFormatter, int tracerFlags)
  {
    super(txa);
    this.metricNameFormat = metricNameFormatter;
    this.classMethodSignature = sig;
    this.startTime = System.nanoTime();
    this.invocationTarget = object;
    this.parentTracer = txa.getLastTracer();
    if (!txa.canCreateTransactionSegment())
    {
      tracerFlags = TracerFlags.clearSegment(tracerFlags);
    }

    this.tracerFlags = ((byte)tracerFlags);
  }

  public DefaultTracer(Transaction transaction, ClassMethodSignature sig, Object object, MetricNameFormat metricNameFormatter)
  {
    this(transaction, sig, object, metricNameFormatter, 6);
  }

  public DefaultTracer(Transaction transaction, ClassMethodSignature sig, Object object) {
    this(transaction, sig, object, NULL_METRIC_NAME_FORMATTER);
  }

  public void finish(Throwable throwable)
  {
    if (!getTransaction().getTransactionState().finish(getTransaction(), this))
      return;
    try
    {
      getTransactionActivity().lockTracerStart();
      doFinish(throwable);
    } catch (Throwable t) {
      String msg = MessageFormat.format("An error occurred finishing tracer for class {0} : {1}", new Object[] { this.classMethodSignature.getClassName(), t });

      if (Agent.LOG.isLoggable(Level.FINER))
        Agent.LOG.log(Level.WARNING, msg, t);
      else
        Agent.LOG.warning(msg);
    }
    finally {
      getTransactionActivity().unlockTracerStart();
    }

    finish(191, null);

    if (Agent.isDebugEnabled())
      Agent.LOG.log(Level.FINE, "(Debug) Tracer.finish(Throwable)");
  }

  protected void reset()
  {
    this.invocationTarget = null;
  }

  public void finish(int opcode, Object returnValue)
  {
    if (!getTransaction().getTransactionState().finish(getTransaction(), this)) {
      return;
    }

    this.duration = Math.max(0L, System.nanoTime() - getStartTime());
    this.exclusiveDuration += this.duration;
    if ((this.exclusiveDuration < 0L) || (this.exclusiveDuration > this.duration)) {
      String msg = MessageFormat.format("Invalid exclusive time {0} for tracer {1}", new Object[] { Long.valueOf(this.exclusiveDuration), getClass().getName() });

      Agent.LOG.severe(msg);
      this.exclusiveDuration = this.duration;
    }

    getTransactionActivity().lockTracerStart();
    try {
      try {
        if (191 != opcode)
          doFinish(opcode, returnValue);
      }
      catch (Throwable t) {
        String msg = MessageFormat.format("An error occurred finishing tracer for class {0} : {1}", new Object[] { this.classMethodSignature.getClassName(), t.toString() });

        Agent.LOG.severe(msg);
        Agent.LOG.log(Level.FINER, msg, t);
      }
      try
      {
        attemptToStoreStackTrace();
      } catch (Throwable t) {
        if (Agent.LOG.isFinestEnabled()) {
          String msg = MessageFormat.format("An error occurred getting stack trace for class {0} : {1}", new Object[] { this.classMethodSignature.getClassName(), t.toString() });

          Agent.LOG.log(Level.FINEST, msg, t);
        }
      }

      if (this.parentTracer != null) {
        this.parentTracer.childTracerFinished(this);
      }
      try
      {
        recordMetrics(getTransactionActivity().getTransactionStats());
      } catch (Throwable t) {
        String msg = MessageFormat.format("An error occurred recording tracer metrics for class {0} : {1}", new Object[] { this.classMethodSignature.getClassName(), t.toString() });

        Agent.LOG.severe(msg);
        Agent.LOG.log(Level.FINER, msg, t);
      }
      try
      {
        if (!(this instanceof SkipTracer))
          getTransactionActivity().tracerFinished(this, opcode);
      }
      catch (Throwable t) {
        String msg = MessageFormat.format("An error occurred calling Transaction.tracerFinished() for class {0} : {1}", new Object[] { this.classMethodSignature.getClassName(), t.toString() });

        Agent.LOG.severe(msg);
        Agent.LOG.log(Level.FINER, msg, t);
      }
      reset();
    } finally {
      getTransactionActivity().unlockTracerStart();
    }
  }

  protected void doFinish(Throwable throwable)
  {
  }

  protected void doFinish(int opcode, Object returnValue)
  {
  }

  protected boolean shouldStoreStackTrace()
  {
    return isTransactionSegment();
  }

  private void attemptToStoreStackTrace() {
    if (shouldStoreStackTrace()) {
      TransactionTracerConfig transactionTracerConfig = getTransaction().getTransactionTracerConfig();
      double stackTraceThresholdInNanos = transactionTracerConfig.getStackTraceThresholdInNanos();
      int stackTraceMax = transactionTracerConfig.getMaxStackTraces();

      if ((getDuration() > stackTraceThresholdInNanos) && ((this.childHasStackTrace) || (getTransaction().getTransactionCounts().getStackTraceCount() < stackTraceMax)))
      {
        storeStackTrace();

        if (!this.childHasStackTrace) {
          getTransaction().getTransactionCounts().incrementStackTraceCount();

          this.childHasStackTrace = true;
        }
      }
    }
  }

  public void storeStackTrace()
  {
    setAttribute("backtrace", Thread.currentThread().getStackTrace());
  }

  public void setAttribute(String key, Object value)
  {
    if (getTransaction().getTransactionCounts().isOverTracerSegmentLimit()) {
      return;
    }
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

  static int sizeof(Object value) {
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

  public Map<String, Object> getAttributes()
  {
    if (this.attributes == null) {
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(this.attributes);
  }

  public long getRunningDurationInNanos()
  {
    return this.duration > 0L ? this.duration : Math.max(0L, System.nanoTime() - getStartTime());
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
    return this.exclusiveDuration;
  }

  public long getEndTime()
  {
    return getStartTime() + this.duration;
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

  protected final Object getInvocationTarget()
  {
    return this.invocationTarget;
  }

  public Tracer getParentTracer()
  {
    return this.parentTracer;
  }

  public void setParentTracer(Tracer tracer)
  {
    this.parentTracer = tracer;
  }

  public String getRequestMetricName() {
    return null;
  }

  public void setMetricNameFormat(MetricNameFormat nameFormat) {
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
    if ((child.isMetricProducer()) && (!(child instanceof SkipTracer))) {
      this.exclusiveDuration -= child.getDuration();
      if ((isTransactionSegment()) && (child.isTransactionSegment())) {
        this.isParent = true;
        if (child.isChildHasStackTrace())
          this.childHasStackTrace = true;
      }
    }
  }

  public void childTracerFinished(long childDurationInNanos)
  {
    this.exclusiveDuration -= childDurationInNanos;
  }

  public ClassMethodSignature getClassMethodSignature()
  {
    return this.classMethodSignature;
  }

  public final boolean isTransactionSegment()
  {
    return (this.tracerFlags & 0x4) == 4;
  }

  public boolean isMetricProducer()
  {
    return (this.tracerFlags & 0x2) == 2;
  }

  public final boolean isLeaf()
  {
    return (this.tracerFlags & 0x20) == 32;
  }

  public boolean isChildHasStackTrace()
  {
    return this.childHasStackTrace;
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
    MetricNameFormat format = new SimpleMetricNameFormat(metricName, transactionSegmentName, transactionSegmentUri);
    setMetricNameFormat(format);
  }
}