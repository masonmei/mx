package com.newrelic.agent.tracers;

import java.util.Collections;
import java.util.Map;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.trace.TransactionSegment;

public abstract class MethodExitTracerNoSkip extends AbstractTracer {
    private final ClassMethodSignature signature;
    protected Tracer parentTracer;

    public MethodExitTracerNoSkip(ClassMethodSignature signature, Transaction transaction) {
        super(transaction);
        this.signature = signature;
        parentTracer = (transaction == null ? null : transaction.getTransactionActivity().getLastTracer());
    }

    public MethodExitTracerNoSkip(ClassMethodSignature signature, TransactionActivity activity) {
        super(activity);
        this.signature = signature;
        parentTracer = (activity == null ? null : activity.getLastTracer());
    }

    protected abstract void doFinish(int paramInt, Object paramObject);

    public void childTracerFinished(Tracer child) {
    }

    public final void finish(int opcode, Object returnValue) {
        try {
            doFinish(opcode, returnValue);
        } finally {
            if (getTransaction() != null) {
                getTransaction().getTransactionActivity().tracerFinished(this, opcode);
            }
        }
    }

    public void finish(Throwable throwable) {
    }

    public Tracer getParentTracer() {
        return parentTracer;
    }

    public void setParentTracer(Tracer tracer) {
        parentTracer = tracer;
    }

    public final ClassMethodSignature getClassMethodSignature() {
        return signature;
    }

    public final long getDurationInMilliseconds() {
        return 0L;
    }

    public final long getRunningDurationInNanos() {
        return 0L;
    }

    public final long getDuration() {
        return 0L;
    }

    public final long getExclusiveDuration() {
        return 0L;
    }

    public final long getStartTime() {
        return 0L;
    }

    public final long getStartTimeInMilliseconds() {
        return 0L;
    }

    public final long getEndTime() {
        return 0L;
    }

    public final long getEndTimeInMilliseconds() {
        return 0L;
    }

    public final String getMetricName() {
        return null;
    }

    public void setMetricName(String[] metricNameParts) {
    }

    public String getTransactionSegmentName() {
        return null;
    }

    public String getTransactionSegmentUri() {
        return null;
    }

    public final Map<String, Object> getAttributes() {
        return Collections.emptyMap();
    }

    public Object getAttribute(String key) {
        return null;
    }

    public void setAttribute(String key, Object value) {
    }

    public final boolean isTransactionSegment() {
        return false;
    }

    public final boolean isMetricProducer() {
        return false;
    }

    public boolean isParent() {
        return false;
    }

    public final TransactionSegment getTransactionSegment(TransactionTracerConfig ttConfig, SqlObfuscator sqlObfuscator,
                                                          long startTime, TransactionSegment lastSibling) {
        return new TransactionSegment(ttConfig, sqlObfuscator, startTime, this);
    }

    public void setMetricNameFormatInfo(String metricName, String transactionSegmentName,
                                        String transactionSegmentUri) {
    }
}