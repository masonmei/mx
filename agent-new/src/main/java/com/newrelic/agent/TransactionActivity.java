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
        this.transaction = tx;
        TransactionTraceService ttService = ServiceFactory.getTransactionTraceService();
        this.tracers = (ttService.isEnabled() ? new ArrayList(128) : null);
        this.transactionStats = new TransactionStats();
        this.transactionCache = new TransactionCache();

        Thread thread = Thread.currentThread();
        this.threadName = thread.getName();

        if (ttService.isEnabled()) {
            if (ttService.isThreadCpuTimeEnabled()) {
                ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
                this.cpuStartTimeInNanos = threadMXBean.getCurrentThreadCpuTime();
                this.totalCpuTimeInNanos = 0L;
            } else {
                this.cpuStartTimeInNanos = -1L;
                this.totalCpuTimeInNanos = -1L;
            }
        } else {
            this.cpuStartTimeInNanos = -1L;
            this.totalCpuTimeInNanos = -1L;
        }
    }

    public TransactionActivity() {
        String realClassName = getClass().getSimpleName();
        if (!realClassName.startsWith("Mock")) {
            throw new IllegalStateException("the public constructor is only for test purposes.");
        }
        this.tracers = null;
        this.transactionStats = null;
        this.transactionCache = null;
        this.threadName = "MockThread";
        this.cpuStartTimeInNanos = -1L;
        this.totalCpuTimeInNanos = -1L;
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
        TransactionActivity result = (TransactionActivity) activityHolder.get();
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
        return this.transaction.shouldGenerateTransactionSegment();
    }

    public Object getContext() {
        if (this.context == null) {
            Agent.LOG.log(Level.FINE, "TransactionActivity: context is null.");
        }
        return this.context;
    }

    public void setContext(Object context) {
        if (context == null) {
            Agent.LOG.log(Level.FINE, "TransactionActivity: context is being set to null.");
        }
        this.context = context;
    }

    public TransactionStats getTransactionStats() {
        return this.transactionStats;
    }

    public List<Tracer> getTracers() {
        return Collections.unmodifiableList(this.tracers);
    }

    public long getTotalCpuTime() {
        return this.totalCpuTimeInNanos;
    }

    public void setToIgnore() {
        this.activityIsIgnored = true;
    }

    void setOwningTransactionIsIgnored(boolean newState) {
        this.activityIsIgnored = newState;
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
            this.lastTracer = tracer;
            addTracer(tracer);
        } else if (Agent.LOG.isFinestEnabled()) {
            Agent.LOG.log(Level.FINEST, "tracerStarted: {0} cannot be added: no parent pointer", new Object[] {tracer});
        }

        return tracer;
    }

    public void tracerFinished(Tracer tracer, int opcode) {
        if ((tracer instanceof SkipTracer)) {
            return;
        }
        if (tracer != this.lastTracer) {
            failed(this, tracer, opcode);
        } else if (tracer == this.rootTracer) {
            finished(this.rootTracer, opcode);
        } else {
            this.lastTracer = tracer.getParentTracer();
        }
    }

    private void failed(TransactionActivity activity, Tracer tracer, int opcode) {
        Agent.LOG.log(Level.SEVERE, "Inconsistent state!  tracer != last tracer for {0} ({1} != {2})",
                             new Object[] {this, tracer, this.lastTracer});
        try {
            this.transaction.activityFailed(this, opcode);
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
            if (!this.activityIsIgnored) {
                recordCpu();
            }
            this.transaction.activityFinished(this, tracer, opcode);
        } finally {
            activityHolder.remove();
        }
    }

    public boolean isStarted() {
        return this.rootTracer != null;
    }

    public boolean isFlyweight() {
        return (this.lastTracer != null) && (this.lastTracer.isLeaf());
    }

    public void recordCpu() {
        if (this.transaction.isTransactionTraceEnabled()) {
            if ((this.cpuStartTimeInNanos > -1L) && (this.totalCpuTimeInNanos == 0L)) {
                this.totalCpuTimeInNanos =
                        (ServiceFactory.getTransactionTraceService().getThreadMXBean().getCurrentThreadCpuTime()
                                 - this.cpuStartTimeInNanos);
            }
        }
    }

    public void addTracer(Tracer tracer) {
        if ((tracer.isTransactionSegment()) && (this.tracers != null)) {
            getTransaction().getTransactionCounts().addTracer();
            this.tracers.add(tracer);
        }
    }

    public void lockTracerStart() {
        this.tracerStartLock -= 1;
    }

    public void unlockTracerStart() {
        this.tracerStartLock += 1;
    }

    public boolean isTracerStartLocked() {
        return this.tracerStartLock < 0;
    }

    public boolean checkTracerStart() {
        if (isTracerStartLocked()) {
            return false;
        }
        if ((!isFlyweight()) && (!this.activityIsIgnored)) {
            lockTracerStart();
            return true;
        }
        return false;
    }

    public Tracer getLastTracer() {
        return this.lastTracer;
    }

    public TracedMethod startFlyweightTracer() {
        try {
            if (this.rootTracer == null) {
                return null;
            }
            Tracer tracer = this.lastTracer;
            if (this.lastTracer.isLeaf()) {
                return null;
            }
            this.lastTracer = FLYWEIGHT_PLACEHOLDER;

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

                if (this.lastTracer == FLYWEIGHT_PLACEHOLDER) {
                    this.lastTracer = parentTracer;
                } else {
                    Agent.LOG.log(Level.FINEST, "Error finishing tracer - the last tracer is of the wrong type.");
                }

                if (duration < 0L) {
                    Agent.LOG.log(Level.FINEST, "A tracer finished with a negative duration.");
                    return;
                }

                this.transactionStats.getScopedStats().getResponseTimeStats(metricName)
                        .recordResponseTimeInNanos(duration);
                Agent.LOG.log(Level.FINEST, "Finished flyweight tracer {0} ({1}.{2}{3})",
                                     new Object[] {metricName, className, methodName, methodDesc});

                if (rollupMetricNames != null) {
                    SimpleStatsEngine unscopedStats = this.transactionStats.getUnscopedStats();
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
            this.rootTracer.setParentTracer(parentTracer);
        } else {
            Agent.LOG.log(Level.FINE, "TranactionActivity.startAsyncActivity: parentTracer is null.");
        }
    }

    public Tracer getRootTracer() {
        return this.rootTracer;
    }

    private void setRootTracer(Tracer tracer) {
        this.rootTracer = tracer;
        this.lastTracer = tracer;

        this.transaction.activityStarted(this);

        if ((tracer instanceof DefaultTracer)) {
            ((DefaultTracer) this.rootTracer).setAttribute("exec_context", this.threadName);
        }

        getTransaction().getTransactionCounts().addTracer();
    }

    public TransactionCache getTransactionCache() {
        return this.transactionCache;
    }

    public Transaction getTransaction() {
        return this.transaction;
    }

    public int hashCode() {
        return this.activityId;
    }
}