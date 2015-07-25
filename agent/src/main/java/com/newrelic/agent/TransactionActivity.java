package com.newrelic.agent;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.SimpleStatsEngine;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.SkipTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TransactionActivityInitiator;
import com.newrelic.agent.transaction.TransactionCache;

public class TransactionActivity {
    public static final int NOT_REPORTED = -1;
    private static final ThreadLocal<TransactionActivity> activityHolder = new ThreadLocal() {
        public TransactionActivity get() {
            return (TransactionActivity) super.get();
        }

        public void set(TransactionActivity value) {
            super.set(value);
        }

        public void remove() {
            super.remove();
        }
    };
    private static final Tracer FLYWEIGHT_PLACEHOLDER = new Tracer() {
        public void setMetricNameFormatInfo(String metricName, String transactionSegmentName,
                                            String transactionSegmentUri) {
        }

        public TracedMethod getParentTracedMethod() {
            return null;
        }

        public void finish(Throwable throwable) {
        }

        public void finish(int opcode, Object returnValue) {
        }

        public long getDurationInMilliseconds() {
            return 0L;
        }

        public long getDuration() {
            return 0L;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return null;
        }

        public boolean isTransactionSegment() {
            return false;
        }

        public boolean isParent() {
            return false;
        }

        public boolean isMetricProducer() {
            return true;
        }

        public boolean isChildHasStackTrace() {
            return false;
        }

        public String getTransactionSegmentUri() {
            return null;
        }

        public String getTransactionSegmentName() {
            return null;
        }

        public TransactionSegment getTransactionSegment(TransactionTracerConfig ttConfig, SqlObfuscator sqlObfuscator,
                                                        long startTime, TransactionSegment lastSibling) {
            return null;
        }

        public long getStartTimeInMilliseconds() {
            return 0L;
        }

        public long getStartTime() {
            return 0L;
        }

        public long getRunningDurationInNanos() {
            return 0L;
        }

        public Tracer getParentTracer() {
            return null;
        }

        public void setParentTracer(Tracer tracer) {
        }

        public Map<String, Object> getAttributes() {
            return Collections.emptyMap();
        }

        public String getMetricName() {
            return null;
        }

        public void setMetricName(String[] metricNameParts) {
        }

        public long getExclusiveDuration() {
            return 0L;
        }

        public long getEndTimeInMilliseconds() {
            return 0L;
        }

        public long getEndTime() {
            return 0L;
        }

        public ClassMethodSignature getClassMethodSignature() {
            return null;
        }

        public void childTracerFinished(Tracer child) {
            throw new UnsupportedOperationException();
        }

        public boolean isLeaf() {
            return true;
        }

        public void setRollupMetricNames(String[] metricNames) {
        }

        public void nameTransaction(TransactionNamePriority namePriority) {
        }

        public void addRollupMetricName(String[] metricNameParts) {
        }

        public void addExclusiveRollupMetricName(String[] metricNameParts) {
        }

        public void setAttribute(String key, Object value) {
        }

        public Object getAttribute(String key) {
            return null;
        }
    };
    private final List<Tracer> tracers;
    private final TransactionStats transactionStats;
    private final TransactionCache transactionCache;
    private final String threadName;
    private final long cpuStartTimeInNanos;
    private Tracer rootTracer;
    private Tracer lastTracer;
    private Transaction transaction;
    private long totalCpuTimeInNanos;
    private int tracerStartLock;
    private volatile boolean activityIsIgnored = false;
    private int activityId;
    private Object context = null;

    private TransactionActivity(Transaction tx) {
        transaction = tx;
        TransactionTraceService ttService = ServiceFactory.getTransactionTraceService();
        tracers = (ttService.isEnabled() ? new ArrayList<Tracer>(128) : null);
        transactionStats = new TransactionStats();
        transactionCache = new TransactionCache();

        Thread thread = Thread.currentThread();
        threadName = thread.getName();

        if (ttService.isEnabled()) {
            if (ttService.isThreadCpuTimeEnabled()) {
                ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
                cpuStartTimeInNanos = threadMXBean.getCurrentThreadCpuTime();
                totalCpuTimeInNanos = 0L;
            } else {
                cpuStartTimeInNanos = -1L;
                totalCpuTimeInNanos = -1L;
            }
        } else {
            cpuStartTimeInNanos = -1L;
            totalCpuTimeInNanos = -1L;
        }
    }

    public TransactionActivity() {
        String realClassName = getClass().getSimpleName();
        if (!realClassName.startsWith("Mock")) {
            throw new IllegalStateException("the public constructor is only for test purposes.");
        }
        tracers = null;
        transactionStats = null;
        transactionCache = null;
        threadName = "MockThread";
        cpuStartTimeInNanos = -1L;
        totalCpuTimeInNanos = -1L;
    }

    public static void clear() {
        activityHolder.remove();
        Agent.LOG.log(Level.FINEST, "TransactionActivity.clear()");
    }

    public static void set(TransactionActivity txa) {
        activityHolder.set(txa);
        Agent.LOG.log(Level.FINEST, "TransactionActivity.set({0})", new Object[] {txa});
    }

    public static TransactionActivity get() {
        TransactionActivity result = activityHolder.get();
        return result;
    }

    public static TransactionActivity create(Transaction transaction, int id) {
        TransactionActivity txa = new TransactionActivity(transaction);
        txa.activityId = id;
        activityHolder.set(txa);
        Agent.LOG.log(Level.FINE, "created {0} for {1}", new Object[] {txa, transaction});
        return txa;
    }

    public boolean canCreateTransactionSegment() {
        return transaction.shouldGenerateTransactionSegment();
    }

    public Object getContext() {
        if (context == null) {
            Agent.LOG.log(Level.FINE, "TransactionActivity: context is null.");
        }
        return context;
    }

    public void setContext(Object context) {
        if (context == null) {
            Agent.LOG.log(Level.FINE, "TransactionActivity: context is being set to null.");
        }
        this.context = context;
    }

    public TransactionStats getTransactionStats() {
        return transactionStats;
    }

    public List<Tracer> getTracers() {
        return Collections.unmodifiableList(tracers);
    }

    public long getTotalCpuTime() {
        return totalCpuTimeInNanos;
    }

    public void setToIgnore() {
        activityIsIgnored = true;
    }

    void setOwningTransactionIsIgnored(boolean newState) {
        activityIsIgnored = newState;
    }

    public Tracer tracerStarted(Tracer tracer) {
        if (isTracerStartLocked()) {
            Agent.LOG.log(Level.FINER, "tracerStarted ignored: tracerStartLock is already active");
            return null;
        }

        if (!isStarted()) {
            if ((tracer instanceof TransactionActivityInitiator)) {
                setRootTracer(tracer);
            } else {
                return null;
            }
        } else if (tracer.getParentTracer() != null) {
            lastTracer = tracer;
            addTracer(tracer);
        } else if (Agent.LOG.isFinestEnabled()) {
            Agent.LOG.log(Level.FINEST, "tracerStarted: {0} cannot be added: no parent pointer", tracer);
        }

        return tracer;
    }

    public void tracerFinished(Tracer tracer, int opcode) {
        if ((tracer instanceof SkipTracer)) {
            return;
        }
        if (tracer != lastTracer) {
            failed(this, tracer, opcode);
        } else if (tracer == rootTracer) {
            finished(rootTracer, opcode);
        } else {
            lastTracer = tracer.getParentTracer();
        }
    }

    private void failed(TransactionActivity activity, Tracer tracer, int opcode) {
        Agent.LOG.log(Level.SEVERE, "Inconsistent state!  tracer != last tracer for {0} ({1} != {2})", this, tracer,
                             lastTracer);
        try {
            transaction.activityFailed(this, opcode);
        } finally {
            activityHolder.remove();
        }
    }

    private void finished(Tracer tracer, int opcode) {
        if (Agent.LOG.isFinestEnabled()) {
            Agent.LOG.log(Level.FINEST, "tracerFinished: {0} opcode: {1} in transactionActivity {2}",
                                 new Object[] {tracer, Integer.valueOf(opcode), this});
        }
        try {
            if (!activityIsIgnored) {
                recordCpu();
            }
            transaction.activityFinished(this, tracer, opcode);
        } finally {
            activityHolder.remove();
        }
    }

    public boolean isStarted() {
        return rootTracer != null;
    }

    public boolean isFlyweight() {
        return (lastTracer != null) && (lastTracer.isLeaf());
    }

    public void recordCpu() {
        if (transaction.isTransactionTraceEnabled()) {
            if ((cpuStartTimeInNanos > -1L) && (totalCpuTimeInNanos == 0L)) {
                totalCpuTimeInNanos =
                        (ServiceFactory.getTransactionTraceService().getThreadMXBean().getCurrentThreadCpuTime()
                                 - cpuStartTimeInNanos);
            }
        }
    }

    public void addTracer(Tracer tracer) {
        if ((tracer.isTransactionSegment()) && (tracers != null)) {
            getTransaction().getTransactionCounts().addTracer();
            tracers.add(tracer);
        }
    }

    public void lockTracerStart() {
        tracerStartLock -= 1;
    }

    public void unlockTracerStart() {
        tracerStartLock += 1;
    }

    public boolean isTracerStartLocked() {
        return tracerStartLock < 0;
    }

    public boolean checkTracerStart() {
        if (isTracerStartLocked()) {
            return false;
        }
        if ((!isFlyweight()) && (!activityIsIgnored)) {
            lockTracerStart();
            return true;
        }
        return false;
    }

    public Tracer getLastTracer() {
        return lastTracer;
    }

    public TracedMethod startFlyweightTracer() {
        try {
            if (rootTracer == null) {
                return null;
            }
            Tracer tracer = lastTracer;
            if (lastTracer.isLeaf()) {
                return null;
            }
            lastTracer = FLYWEIGHT_PLACEHOLDER;

            return tracer;
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINEST, t, "Error starting tracer", new Object[0]);
        }
        return null;
    }

    public void finishFlyweightTracer(TracedMethod parent, long startInNanos, long finishInNanos, String className,
                                      String methodName, String methodDesc, String metricName,
                                      String[] rollupMetricNames) {
        try {
            if ((parent instanceof DefaultTracer)) {
                DefaultTracer parentTracer = (DefaultTracer) parent;

                long duration = finishInNanos - startInNanos;

                if (lastTracer == FLYWEIGHT_PLACEHOLDER) {
                    lastTracer = parentTracer;
                } else {
                    Agent.LOG.log(Level.FINEST, "Error finishing tracer - the last tracer is of the wrong type.");
                }

                if (duration < 0L) {
                    Agent.LOG.log(Level.FINEST, "A tracer finished with a negative duration.");
                    return;
                }

                transactionStats.getScopedStats().getResponseTimeStats(metricName).recordResponseTimeInNanos(duration);
                Agent.LOG.log(Level.FINEST, "Finished flyweight tracer {0} ({1}.{2}{3})",
                                     new Object[] {metricName, className, methodName, methodDesc});

                if (rollupMetricNames != null) {
                    SimpleStatsEngine unscopedStats = transactionStats.getUnscopedStats();
                    for (String name : rollupMetricNames) {
                        unscopedStats.getResponseTimeStats(name).recordResponseTimeInNanos(duration);
                    }
                }

                parentTracer.childTracerFinished(duration);
            }
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINEST, t, "Error finishing tracer", new Object[0]);
        }
    }

    public void startAsyncActivity(Object context, Transaction transaction, int activityId, Tracer parentTracer) {
        setContext(context);
        this.transaction = transaction;
        this.activityId = activityId;

        if (parentTracer != null) {
            rootTracer.setParentTracer(parentTracer);
        } else {
            Agent.LOG.log(Level.FINE, "TranactionActivity.startAsyncActivity: parentTracer is null.");
        }
    }

    public Tracer getRootTracer() {
        return rootTracer;
    }

    private void setRootTracer(Tracer tracer) {
        rootTracer = tracer;
        lastTracer = tracer;

        transaction.activityStarted(this);

        if ((tracer instanceof DefaultTracer)) {
            ((DefaultTracer) rootTracer).setAttribute("exec_context", threadName);
        }

        getTransaction().getTransactionCounts().addTracer();
    }

    public TransactionCache getTransactionCache() {
        return transactionCache;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public int hashCode() {
        return activityId;
    }
}