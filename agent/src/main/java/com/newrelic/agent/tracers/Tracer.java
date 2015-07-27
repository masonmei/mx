package com.newrelic.agent.tracers;

import java.lang.reflect.InvocationHandler;
import java.util.Map;

import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.trace.TransactionSegment;

public interface Tracer extends InvocationHandler, TimedItem, ExitTracer {
    long getStartTime();

    long getStartTimeInMilliseconds();

    long getEndTime();

    long getEndTimeInMilliseconds();

    long getExclusiveDuration();

    long getRunningDurationInNanos();

    String getMetricName();

    String getTransactionSegmentName();

    String getTransactionSegmentUri();

    Map<String, Object> getAttributes();

    void setAttribute(String paramString, Object paramObject);

    Object getAttribute(String paramString);

    void childTracerFinished(Tracer paramTracer);

    Tracer getParentTracer();

    void setParentTracer(Tracer paramTracer);

    boolean isParent();

    boolean isMetricProducer();

    ClassMethodSignature getClassMethodSignature();

    boolean isTransactionSegment();

    boolean isChildHasStackTrace();

    TransactionSegment getTransactionSegment(TransactionTracerConfig paramTransactionTracerConfig,
                                             SqlObfuscator paramSqlObfuscator, long paramLong,
                                             TransactionSegment paramTransactionSegment);

    boolean isLeaf();
}