package com.newrelic.agent.tracers;

import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.trace.TransactionSegment;
import java.lang.reflect.InvocationHandler;
import java.util.Map;

public abstract interface Tracer extends InvocationHandler, TimedItem, ExitTracer
{
  public abstract long getStartTime();

  public abstract long getStartTimeInMilliseconds();

  public abstract long getEndTime();

  public abstract long getEndTimeInMilliseconds();

  public abstract long getExclusiveDuration();

  public abstract long getRunningDurationInNanos();

  public abstract String getMetricName();

  public abstract String getTransactionSegmentName();

  public abstract String getTransactionSegmentUri();

  public abstract Map<String, Object> getAttributes();

  public abstract void setAttribute(String paramString, Object paramObject);

  public abstract Object getAttribute(String paramString);

  public abstract void childTracerFinished(Tracer paramTracer);

  public abstract Tracer getParentTracer();

  public abstract void setParentTracer(Tracer paramTracer);

  public abstract boolean isParent();

  public abstract boolean isMetricProducer();

  public abstract ClassMethodSignature getClassMethodSignature();

  public abstract boolean isTransactionSegment();

  public abstract boolean isChildHasStackTrace();

  public abstract TransactionSegment getTransactionSegment(TransactionTracerConfig paramTransactionTracerConfig,
                                                           SqlObfuscator paramSqlObfuscator, long paramLong,
                                                           TransactionSegment paramTransactionSegment);

  public abstract boolean isLeaf();
}