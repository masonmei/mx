package com.newrelic.agent.tracers;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.util.Strings;

public class DefaultTracer extends AbstractTracer {
    public static final MetricNameFormat NULL_METRIC_NAME_FORMATTER = new SimpleMetricNameFormat(null);
    public static final String BACKTRACE_PARAMETER_NAME = "backtrace";
    protected static final int DEFAULT_TRACER_FLAGS = 6;
    private static final int INITIAL_PARAMETER_MAP_SIZE = 5;
    private final long startTime;
    private final ClassMethodSignature classMethodSignature;
    private final byte tracerFlags;
    private long duration;
    private long exclusiveDuration;
    private Map<String, Object> attributes;
    private Tracer parentTracer;
    private Object invocationTarget;
    private MetricNameFormat metricNameFormat;
    private boolean isParent;
    private boolean childHasStackTrace;

    public DefaultTracer(Transaction transaction, ClassMethodSignature sig, Object object,
                         MetricNameFormat metricNameFormatter, int tracerFlags) {
        this(transaction.getTransactionActivity(), sig, object, metricNameFormatter, tracerFlags);
    }

    public DefaultTracer(TransactionActivity txa, ClassMethodSignature sig, Object object,
                         MetricNameFormat metricNameFormatter, int tracerFlags) {
        super(txa);
        metricNameFormat = metricNameFormatter;
        classMethodSignature = sig;
        startTime = System.nanoTime();
        invocationTarget = object;
        parentTracer = txa.getLastTracer();
        if (!txa.canCreateTransactionSegment()) {
            tracerFlags = TracerFlags.clearSegment(tracerFlags);
        }

        this.tracerFlags = ((byte) tracerFlags);
    }

    public DefaultTracer(Transaction transaction, ClassMethodSignature sig, Object object,
                         MetricNameFormat metricNameFormatter) {
        this(transaction, sig, object, metricNameFormatter, 6);
    }

    public DefaultTracer(Transaction transaction, ClassMethodSignature sig, Object object) {
        this(transaction, sig, object, NULL_METRIC_NAME_FORMATTER);
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

    public void finish(Throwable throwable) {
        if (!getTransaction().getTransactionState().finish(getTransaction(), this)) {
            return;
        }
        try {
            getTransactionActivity().lockTracerStart();
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
            getTransactionActivity().unlockTracerStart();
        }

        finish(191, null);

        if (Agent.isDebugEnabled()) {
            Agent.LOG.log(Level.FINE, "(Debug) Tracer.finish(Throwable)");
        }
    }

    protected void reset() {
        invocationTarget = null;
    }

    public void finish(int opcode, Object returnValue) {
        if (!getTransaction().getTransactionState().finish(getTransaction(), this)) {
            return;
        }

        duration = Math.max(0L, System.nanoTime() - getStartTime());
        exclusiveDuration += duration;
        if ((exclusiveDuration < 0L) || (exclusiveDuration > duration)) {
            String msg = MessageFormat.format("Invalid exclusive time {0} for tracer {1}",
                                                     new Object[] {Long.valueOf(exclusiveDuration),
                                                                          getClass().getName()});

            Agent.LOG.severe(msg);
            exclusiveDuration = duration;
        }

        getTransactionActivity().lockTracerStart();
        try {
            try {
                if (191 != opcode) {
                    doFinish(opcode, returnValue);
                }
            } catch (Throwable t) {
                String msg = MessageFormat.format("An error occurred finishing tracer for class {0} : {1}",
                                                         new Object[] {classMethodSignature.getClassName(),
                                                                              t.toString()});

                Agent.LOG.severe(msg);
                Agent.LOG.log(Level.FINER, msg, t);
            }
            try {
                attemptToStoreStackTrace();
            } catch (Throwable t) {
                if (Agent.LOG.isFinestEnabled()) {
                    String msg = MessageFormat.format("An error occurred getting stack trace for class {0} : {1}",
                                                             new Object[] {classMethodSignature.getClassName(),
                                                                                  t.toString()});

                    Agent.LOG.log(Level.FINEST, msg, t);
                }
            }

            if (parentTracer != null) {
                parentTracer.childTracerFinished(this);
            }
            try {
                recordMetrics(getTransactionActivity().getTransactionStats());
            } catch (Throwable t) {
                String msg = MessageFormat.format("An error occurred recording tracer metrics for class {0} : {1}",
                                                         new Object[] {classMethodSignature.getClassName(),
                                                                              t.toString()});

                Agent.LOG.severe(msg);
                Agent.LOG.log(Level.FINER, msg, t);
            }
            try {
                if (!(this instanceof SkipTracer)) {
                    getTransactionActivity().tracerFinished(this, opcode);
                }
            } catch (Throwable t) {
                String msg = MessageFormat
                                     .format("An error occurred calling Transaction.tracerFinished() for class {0} : "
                                                     + "{1}",
                                                    new Object[] {classMethodSignature.getClassName(), t.toString()});

                Agent.LOG.severe(msg);
                Agent.LOG.log(Level.FINER, msg, t);
            }
            reset();
        } finally {
            getTransactionActivity().unlockTracerStart();
        }
    }

    protected void doFinish(Throwable throwable) {
    }

    protected void doFinish(int opcode, Object returnValue) {
    }

    protected boolean shouldStoreStackTrace() {
        return isTransactionSegment();
    }

    private void attemptToStoreStackTrace() {
        if (shouldStoreStackTrace()) {
            TransactionTracerConfig transactionTracerConfig = getTransaction().getTransactionTracerConfig();
            double stackTraceThresholdInNanos = transactionTracerConfig.getStackTraceThresholdInNanos();
            int stackTraceMax = transactionTracerConfig.getMaxStackTraces();

            if ((getDuration() > stackTraceThresholdInNanos) && ((childHasStackTrace) || (getTransaction()
                                                                                                  .getTransactionCounts()
                                                                                                  .getStackTraceCount()
                                                                                                  < stackTraceMax))) {
                storeStackTrace();

                if (!childHasStackTrace) {
                    getTransaction().getTransactionCounts().incrementStackTraceCount();

                    childHasStackTrace = true;
                }
            }
        }
    }

    public void storeStackTrace() {
        setAttribute("backtrace", Thread.currentThread().getStackTrace());
    }

    public void setAttribute(String key, Object value) {
        if (getTransaction().getTransactionCounts().isOverTracerSegmentLimit()) {
            return;
        }
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
        return duration > 0L ? duration : Math.max(0L, System.nanoTime() - getStartTime());
    }

    public long getDurationInMilliseconds() {
        return TimeUnit.MILLISECONDS.convert(getDuration(), TimeUnit.NANOSECONDS);
    }

    public long getDuration() {
        return duration;
    }

    public long getExclusiveDuration() {
        return exclusiveDuration;
    }

    public long getEndTime() {
        return getStartTime() + duration;
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

    protected final Object getInvocationTarget() {
        return invocationTarget;
    }

    public Tracer getParentTracer() {
        return parentTracer;
    }

    public void setParentTracer(Tracer tracer) {
        parentTracer = tracer;
    }

    public String getRequestMetricName() {
        return null;
    }

    protected final MetricNameFormat getMetricNameFormat() {
        return metricNameFormat;
    }

    public void setMetricNameFormat(MetricNameFormat nameFormat) {
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
        if ((child.isMetricProducer()) && (!(child instanceof SkipTracer))) {
            exclusiveDuration -= child.getDuration();
            if ((isTransactionSegment()) && (child.isTransactionSegment())) {
                isParent = true;
                if (child.isChildHasStackTrace()) {
                    childHasStackTrace = true;
                }
            }
        }
    }

    public void childTracerFinished(long childDurationInNanos) {
        exclusiveDuration -= childDurationInNanos;
    }

    public ClassMethodSignature getClassMethodSignature() {
        return classMethodSignature;
    }

    public final boolean isTransactionSegment() {
        return (tracerFlags & 0x4) == 4;
    }

    public boolean isMetricProducer() {
        return (tracerFlags & 0x2) == 2;
    }

    public final boolean isLeaf() {
        return (tracerFlags & 0x20) == 32;
    }

    public boolean isChildHasStackTrace() {
        return childHasStackTrace;
    }

    public TransactionSegment getTransactionSegment(TransactionTracerConfig ttConfig, SqlObfuscator sqlObfuscator,
                                                    long startTime, TransactionSegment lastSibling) {
        return new TransactionSegment(ttConfig, sqlObfuscator, startTime, this);
    }

    public void setMetricNameFormatInfo(String metricName, String transactionSegmentName,
                                        String transactionSegmentUri) {
        MetricNameFormat format = new SimpleMetricNameFormat(metricName, transactionSegmentName, transactionSegmentUri);
        setMetricNameFormat(format);
    }
}