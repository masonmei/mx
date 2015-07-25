package com.newrelic.agent.async;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.tracers.AbstractTracer;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.SkipTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.util.Strings;

public class AsyncTracer extends AbstractTracer {
    private static final int INITIAL_PARAMETER_MAP_SIZE = 5;
    private final long startTime;
    private final long duration;
    private final ClassMethodSignature classMethodSignature;
    private final boolean metricProducer;
    private final TransactionActivity tracerParentActivty;
    private Map<String, Object> attributes;
    private volatile Tracer parentTracer;
    private MetricNameFormat metricNameFormat;
    private boolean isParent;

    public AsyncTracer(TransactionActivity rootTxActivty, TransactionActivity tracerParentActivty,
                       ClassMethodSignature sig, MetricNameFormat metricNameFormatter, long startTime, long endTime) {
        super(rootTxActivty);
        this.tracerParentActivty = tracerParentActivty;
        this.startTime = startTime;
        metricNameFormat = metricNameFormatter;
        classMethodSignature = sig;
        parentTracer = rootTxActivty.getLastTracer();
        metricProducer = true;
        duration = Math.max(0L, endTime - startTime);
    }

    static int sizeof(Object value) {
        int size = 0;
        if (value == null) {
            return 0;
        }
        if ((value instanceof String)) {
            return ((String) value).length();
        }
        if ((value instanceof StackTraceElement)) {
            StackTraceElement elem = (StackTraceElement) value;

            return sizeof(elem.getClassName()) + sizeof(elem.getFileName()) + sizeof(elem.getMethodName()) + 10;
        }
        if ((value instanceof Object[])) {
            for (Object obj : (Object[]) value) {
                size += sizeof(obj);
            }
        }
        return size;
    }

    public final void finish(Throwable throwable) {
        TransactionActivity activity = getTransaction().getTransactionActivity();
        try {
            activity.lockTracerStart();
            doFinish(throwable);
        } catch (Throwable t) {
            String msg = MessageFormat.format("An error occurred finishing tracer for class {0} : {1}",
                                                     new Object[] {classMethodSignature.getClassName(), t});

            if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.log(Level.WARNING, msg, t);
            } else {
                Agent.LOG.warning(msg);
            }
        } finally {
            activity.unlockTracerStart();
        }
        finish(191, null);

        if (Agent.isDebugEnabled()) {
            Agent.LOG.log(Level.FINE, "(Debug) Tracer.finish(Throwable)");
        }
    }

    public void finish(int opcode, Object returnValue) {
        TransactionActivity activity = getTransactionActivity();
        try {
            activity.lockTracerStart();
            if (191 != opcode) {
                doFinish(opcode, returnValue);
            }
        } catch (Throwable t) {
            String msg = MessageFormat.format("An error occurred finishing tracer for class {0} : {1}",
                                                     classMethodSignature.getClassName(), t.toString());

            Agent.LOG.severe(msg);
            Agent.LOG.log(Level.FINER, msg, t);
        } finally {
            activity.unlockTracerStart();
        }

        if (parentTracer != null) {
            parentTracer.childTracerFinished(this);
        }
        try {
            recordMetrics(getTransaction().getTransactionActivity().getTransactionStats());
        } catch (Throwable t) {
            String msg = MessageFormat.format("An error occurred recording tracer metrics for class {0} : {1}",
                                                     classMethodSignature.getClassName(), t.toString());

            Agent.LOG.severe(msg);
            Agent.LOG.log(Level.FINER, msg, t);
        }
        try {
            if (!(this instanceof SkipTracer)) {
                activity.tracerFinished(this, opcode);
            }
        } catch (Throwable t) {
            String msg = MessageFormat
                                 .format("An error occurred calling Transaction.tracerFinished() for class {0} : {1}",
                                                classMethodSignature.getClassName(), t.toString());

            Agent.LOG.severe(msg);
            Agent.LOG.log(Level.FINER, msg, t);
        }
    }

    protected void doFinish(Throwable throwable) {
    }

    protected void doFinish(int opcode, Object returnValue) {
    }

    public void setAttribute(String key, Object value) {
        if (value.getClass().isArray()) {
            value = Arrays.asList((Object[]) value);
        }

        getTransaction().getTransactionCounts().incrementSize(sizeof(value));

        if (attributes == null) {
            attributes = new HashMap(1, 5.0F);
        }
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes == null ? null : attributes.get(key);
    }

    public Map<String, Object> getAttributes() {
        if (attributes == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(attributes);
    }

    public long getRunningDurationInNanos() {
        return duration;
    }

    public long getDurationInMilliseconds() {
        return TimeUnit.MILLISECONDS.convert(getDuration(), TimeUnit.NANOSECONDS);
    }

    public long getDuration() {
        return duration;
    }

    public long getExclusiveDuration() {
        return 0L;
    }

    public long getEndTime() {
        return startTime + duration;
    }

    public long getEndTimeInMilliseconds() {
        return TimeUnit.MILLISECONDS.convert(getEndTime(), TimeUnit.NANOSECONDS);
    }

    public long getStartTime() {
        return startTime;
    }

    public long getStartTimeInMilliseconds() {
        return TimeUnit.MILLISECONDS.convert(getStartTime(), TimeUnit.NANOSECONDS);
    }

    public Tracer getParentTracer() {
        return parentTracer;
    }

    public void setParentTracer(Tracer tracer) {
        parentTracer = tracer;
    }

    public TransactionActivity getTracerParentActivty() {
        return tracerParentActivty;
    }

    public String getRequestMetricName() {
        return null;
    }

    protected final MetricNameFormat getMetricNameFormat() {
        return metricNameFormat;
    }

    protected final void setMetricNameFormat(MetricNameFormat nameFormat) {
        metricNameFormat = nameFormat;
    }

    public final String getMetricName() {
        return metricNameFormat == null ? null : metricNameFormat.getMetricName();
    }

    public void setMetricName(String[] metricNameParts) {
        String metricName = Strings.join('/', metricNameParts);
        setMetricNameFormat(new SimpleMetricNameFormat(metricName));
    }

    public final String getTransactionSegmentName() {
        return metricNameFormat == null ? null : metricNameFormat.getTransactionSegmentName();
    }

    public final String getTransactionSegmentUri() {
        return metricNameFormat == null ? null : metricNameFormat.getTransactionSegmentUri();
    }

    protected void recordMetrics(TransactionStats transactionStats) {
        if (getTransaction().isIgnore()) {
            return;
        }
        if (isMetricProducer()) {
            String metricName = getMetricName();
            if (metricName != null) {
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

    protected void doRecordMetrics(TransactionStats transactionStats) {
    }

    public final boolean isParent() {
        return isParent;
    }

    public void childTracerFinished(Tracer child) {
        isParent = ((child.isMetricProducer()) && (child.isTransactionSegment()) && (!(child instanceof SkipTracer)));
    }

    public ClassMethodSignature getClassMethodSignature() {
        return classMethodSignature;
    }

    public boolean isTransactionSegment() {
        return true;
    }

    public boolean isMetricProducer() {
        return metricProducer;
    }

    public TransactionSegment getTransactionSegment(TransactionTracerConfig ttConfig, SqlObfuscator sqlObfuscator,
                                                    long startTime, TransactionSegment lastSibling) {
        return new TransactionSegment(ttConfig, sqlObfuscator, startTime, this);
    }

    public void setMetricNameFormatInfo(String metricName, String transactionSegmentName,
                                        String transactionSegmentUri) {
    }
}