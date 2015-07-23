//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.async;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import com.google.common.base.Strings;
import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.TransactionState;
import com.newrelic.agent.TransactionStateImpl;
import com.newrelic.agent.instrumentation.pointcuts.TransactionHolder;
import com.newrelic.agent.instrumentation.pointcuts.scala.TransactionHolderDispatcherPointCut;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.AbstractTracer;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

public class AsyncTransactionState extends TransactionStateImpl {
    private static final String ASYNC_WAIT = "Async Wait";
    private static final ClassMethodSignature ASYNC_WAIT_SIG =
            new ClassMethodSignature("NR_ASYNC_WAIT_CLASS", "NR_ASYNC_WAIT_METHOD", "()V");
    private static final MetricNameFormat ASYNC_WAIT_FORMAT = new SimpleMetricNameFormat("Async Wait");
    private static final Object[] ASYNC_TRACER_ARGS = new Object[] {Integer.valueOf(176), null};
    private static final int MAX_DEPTH = 150;
    private final AtomicBoolean isSuspended;
    private final AtomicBoolean isComplete;
    private final Collection<TransactionActivity> asyncTransactionActivitiesComplete;
    private final Collection<Transaction> asyncTransactions;
    private final Collection<TransactionHolder> asyncJobs;
    private final AtomicReference<TransactionActivity> transactionActivityRef;
    private final AtomicReference<TransactionActivity> parentTransactionActivityRef;
    private final AtomicBoolean invalidateAsyncJobs;
    private final Deque<TransactionActivity> mergingActivities;
    private final Deque<AsyncTracer> mergingActivityAsyncTracer;
    private volatile long asyncStartTimeInNanos;
    private volatile long asyncFinishTimeInNanos;

    public AsyncTransactionState(TransactionActivity txa) {
        this(txa, (TransactionActivity) null);
    }

    public AsyncTransactionState(TransactionActivity txa, TransactionActivity parentTransactionActivity) {
        this.isSuspended = new AtomicBoolean(false);
        this.isComplete = new AtomicBoolean(false);
        this.asyncTransactionActivitiesComplete = new ConcurrentLinkedQueue();
        this.asyncTransactions = new ConcurrentLinkedQueue();
        this.asyncJobs = new ConcurrentLinkedQueue();
        this.transactionActivityRef = new AtomicReference();
        this.parentTransactionActivityRef = new AtomicReference();
        this.asyncStartTimeInNanos = -1L;
        this.asyncFinishTimeInNanos = -1L;
        this.invalidateAsyncJobs = new AtomicBoolean();
        this.mergingActivities = new LinkedList();
        this.mergingActivityAsyncTracer = new LinkedList();
        this.transactionActivityRef.set(txa);
        this.parentTransactionActivityRef.set(parentTransactionActivity);
    }

    public boolean finish(Transaction tx, Tracer tracer) {
        if (tracer == tx.getRootTracer() && this.isAsync() && !this.isComplete()) {
            this.asyncStartTimeInNanos = System.nanoTime();
            tx.getTransactionActivity().recordCpu();
            Transaction.clearTransaction();
            this.isSuspended.set(true);
            if (Agent.LOG.isFinestEnabled()) {
                Agent.LOG.finest(MessageFormat.format("Suspended transaction {0}",
                                                             new Object[] {this.transactionActivityRef.get()}));
            }

            return false;
        } else {
            return true;
        }
    }

    boolean isAsync() {
        return !this.asyncJobs.isEmpty() || !this.asyncTransactions.isEmpty();
    }

    private void tryComplete(boolean finishRootTracer) {
        if (this.isAsyncComplete() && this.isComplete.compareAndSet(false, true)) {
            try {
                this.doComplete(finishRootTracer);
            } catch (Exception var4) {
                String msg = MessageFormat.format("Failed to complete transaction {0}: {1}",
                                                         new Object[] {this.transactionActivityRef.get(), var4});
                if (Agent.LOG.isFinestEnabled()) {
                    Agent.LOG.log(Level.FINEST, msg, var4);
                } else {
                    Agent.LOG.finer(msg);
                }
            }
        }

        if (this.parentTransactionActivityRef.get() == null && Agent.LOG.isFinerEnabled()) {
            this.printIncompleteTransactionGraph();
        }

    }

    public boolean isComplete() {
        return this.isComplete.get();
    }

    private boolean isAsyncComplete() {
        return this.asyncJobs.isEmpty() && this.asyncTransactions.isEmpty() && this.isSuspended.get();
    }

    private void doComplete(boolean finishRootTracer) {
        Transaction currentTx = Transaction.getTransaction();
        if (currentTx.isStarted()) {
            if (currentTx == ((TransactionActivity) this.transactionActivityRef.get()).getTransaction()) {
                currentTx = null;
            } else {
                Transaction.clearTransaction();
                Transaction.setTransaction(((TransactionActivity) this.transactionActivityRef.get()).getTransaction());
            }
        }

        this.asyncFinishTimeInNanos = System.nanoTime();
        this.completeTransaction(finishRootTracer);
        if (Agent.LOG.isFinestEnabled()) {
            Agent.LOG.finest(MessageFormat.format("Completed transaction {0}",
                                                         new Object[] {this.transactionActivityRef.get()}));
        }

        if (currentTx != null) {
            Transaction.clearTransaction();
            Transaction.setTransaction(currentTx);
        }

    }

    private void completeTransaction(boolean finishRootTracer) {
        this.mergeAsyncTransactionData();
        if (finishRootTracer) {
            this.finishTracer((AbstractTracer) ((TransactionActivity) this.transactionActivityRef.get())
                                                       .getRootTracer());
        }

        TransactionActivity parentTxActivity = (TransactionActivity) this.parentTransactionActivityRef.get();
        if (parentTxActivity != null) {
            parentTxActivity.getTransaction().getTransactionState()
                    .asyncTransactionFinished((TransactionActivity) this.transactionActivityRef.get());
        }

    }

    private void mergeAsyncTransactionData() {
        Iterator i$ = this.asyncTransactionActivitiesComplete.iterator();

        while (i$.hasNext()) {
            TransactionActivity txa = (TransactionActivity) i$.next();
            this.mergeAsyncTransactionData(txa);
        }

    }

    private void mergeAsyncTransactionData(TransactionActivity childActivity) {
        if (childActivity == this.transactionActivityRef.get()) {
            Agent.LOG.fine("Cannot merge transaction into itself: " + childActivity);
        } else {
            this.mergeStats(childActivity);
            this.mergeParameters(childActivity.getTransaction());
        }
    }

    private void mergeStats(TransactionActivity childActivity) {
        TransactionActivity txa = (TransactionActivity) this.transactionActivityRef.get();
        TransactionStats stats = txa.getTransactionStats();
        stats.getScopedStats().mergeStats(childActivity.getTransactionStats().getScopedStats());
        childActivity.getTransactionStats().getUnscopedStats().getStatsMap().remove("GC/cumulative");
        stats.getUnscopedStats().mergeStats(childActivity.getTransactionStats().getUnscopedStats());
    }

    private void mergeParameters(Transaction tx) {
        Transaction transaction = ((TransactionActivity) this.transactionActivityRef.get()).getTransaction();
        Long cpuTime = (Long) tx.getIntrinsicAttributes().get("cpu_time");
        if (cpuTime != null) {
            transaction.addTotalCpuTimeForLegacy(cpuTime.longValue());
        }

        if (transaction.getUserAttributes().size() + tx.getUserAttributes().size() <= transaction.getAgentConfig()
                                                                                              .getMaxUserParameters()) {
            transaction.getUserAttributes().putAll(tx.getUserAttributes());
        }

    }

    public void mergeAsyncTracers() {
        if (!this.asyncTransactionActivitiesComplete.isEmpty()) {
            if (!this.asyncTransactionActivitiesComplete.isEmpty()) {
                AsyncTracer txa = new AsyncTracer((TransactionActivity) this.transactionActivityRef.get(),
                                                         (TransactionActivity) this.transactionActivityRef.get(),
                                                         ASYNC_WAIT_SIG, ASYNC_WAIT_FORMAT, this.asyncStartTimeInNanos,
                                                         this.asyncFinishTimeInNanos);
                txa.setAttribute("nr_async_wait", Boolean.valueOf(true));
                ((TransactionActivity) this.transactionActivityRef.get()).tracerStarted(txa);
                this.mergingActivityAsyncTracer.push(txa);
            }

            Iterator txa1 = this.asyncTransactionActivitiesComplete.iterator();

            while (txa1.hasNext()) {
                TransactionActivity asyncState = (TransactionActivity) txa1.next();
                this.mergingActivities.push(asyncState);
            }

            while (!this.mergingActivities.isEmpty()) {
                TransactionActivity txa2 = (TransactionActivity) this.mergingActivities.pop();
                AsyncTransactionState asyncState1 = null;
                if (txa2.getTransaction().getTransactionState() instanceof AsyncTransactionState) {
                    asyncState1 = (AsyncTransactionState) txa2.getTransaction().getTransactionState();
                }

                if (asyncState1 != null) {
                    while (((AsyncTracer) this.mergingActivityAsyncTracer.peek()).getTracerParentActivty()
                                   != asyncState1.parentTransactionActivityRef.get()) {
                        this.finishTracer((AbstractTracer) this.mergingActivityAsyncTracer.pop());
                    }
                }

                this.mergeActivityTracers((AsyncTracer) this.mergingActivityAsyncTracer.peek(), txa2);
                if (asyncState1 != null) {
                    if (!asyncState1.asyncTransactionActivitiesComplete.isEmpty()) {
                        AsyncTracer i$ = new AsyncTracer((TransactionActivity) this.transactionActivityRef.get(), txa2,
                                                                ASYNC_WAIT_SIG, ASYNC_WAIT_FORMAT,
                                                                asyncState1.asyncStartTimeInNanos,
                                                                asyncState1.asyncFinishTimeInNanos);
                        i$.setAttribute("nr_async_wait", Boolean.valueOf(true));
                        ((TransactionActivity) this.transactionActivityRef.get()).tracerStarted(i$);
                        this.mergingActivityAsyncTracer.push(i$);
                    }

                    Iterator i$1 = asyncState1.asyncTransactionActivitiesComplete.iterator();

                    while (i$1.hasNext()) {
                        TransactionActivity childActivity = (TransactionActivity) i$1.next();
                        this.mergingActivities.push(childActivity);
                    }

                    asyncState1.asyncTransactionActivitiesComplete.clear();
                }
            }

            while (!this.mergingActivityAsyncTracer.isEmpty()) {
                this.finishTracer((AbstractTracer) this.mergingActivityAsyncTracer.pop());
            }

        }
    }

    private void finishTracer(AbstractTracer tracer) {
        if (tracer != null) {
            tracer.invoke("s", (Method) null, ASYNC_TRACER_ARGS);
        }

    }

    private void mergeActivityTracers(AsyncTracer asyncWaitTracer, TransactionActivity childActivity) {
        if (childActivity == this.transactionActivityRef.get()) {
            Agent.LOG.fine("Cannot merge transaction into itself: " + childActivity);
        } else {
            this.mergeTracers(asyncWaitTracer, childActivity);
        }
    }

    private void mergeTracers(AsyncTracer asyncWaitTracer, TransactionActivity childActivity) {
        TransactionActivity txa = (TransactionActivity) this.transactionActivityRef.get();
        Tracer rootTracer = childActivity.getRootTracer();
        rootTracer.setParentTracer(asyncWaitTracer);
        txa.addTracer(rootTracer);
        List tracers = this.getInterestingTracers(childActivity);
        Iterator i$ = tracers.iterator();

        while (i$.hasNext()) {
            Tracer tracer = (Tracer) i$.next();
            txa.addTracer(tracer);
        }

        asyncWaitTracer.childTracerFinished(rootTracer);
    }

    private List<Tracer> getInterestingTracers(TransactionActivity txa) {
        Tracer rootTracer = txa.getRootTracer();
        List tracers = txa.getTracers();
        HashSet interestingTracers = new HashSet();
        Iterator i$ = tracers.iterator();

        while (true) {
            Tracer tracer;
            do {
                if (!i$.hasNext()) {
                    return this.getInterestingTracersInStartOrder(tracers, interestingTracers);
                }

                tracer = (Tracer) i$.next();
            } while (!this.isInteresting(tracer.getMetricName()));

            interestingTracers.add(tracer);

            for (Tracer parentTracer = tracer.getParentTracer(); parentTracer != null && parentTracer != rootTracer;
                 parentTracer = parentTracer.getParentTracer()) {
                interestingTracers.add(parentTracer);
            }
        }
    }

    private boolean isInteresting(String metricName) {
        if (metricName != null) {
            if (metricName.startsWith("Async Wait")) {
                return false;
            }

            if (metricName.startsWith("Java/scala.concurrent")) {
                return false;
            }

            if (metricName.startsWith("Java/scala.collection")) {
                return false;
            }

            if (metricName.startsWith("Java/play.api.libs.concurrent")) {
                return false;
            }

            if (metricName.startsWith("Java/play.api.libs.iteratee")) {
                return false;
            }

            if (metricName.startsWith("Java/play.core.server.netty.PlayDefaultUpstreamHandler")) {
                return false;
            }

            if (metricName.startsWith("Java/play.libs.F")) {
                return false;
            }

            if (metricName.startsWith("Java/akka.pattern.PromiseActorRef")) {
                return false;
            }
        }

        return true;
    }

    private List<Tracer> getInterestingTracersInStartOrder(List<Tracer> tracers, Set<Tracer> interestingTracers) {
        ArrayList result = new ArrayList(interestingTracers.size());
        Iterator i$ = tracers.iterator();

        while (i$.hasNext()) {
            Tracer tracer = (Tracer) i$.next();
            if (interestingTracers.contains(tracer)) {
                result.add(tracer);
            }
        }

        return result;
    }

    public void asyncTransactionStarted(Transaction tx, TransactionHolder txHolder) {
        if (!this.isComplete()) {
            if (((TransactionActivity) this.transactionActivityRef.get()).getTransaction() == tx) {
                Agent.LOG.fine("Cannot start async transaction of itself: " + tx);
            } else {
                boolean added = this.addAsyncTransaction(tx);
                if (added && Agent.LOG.isFinestEnabled()) {
                    String msg = MessageFormat.format("Async transaction started for {0} by {1}: {2}",
                                                             new Object[] {this.transactionActivityRef.get(), txHolder,
                                                                                  tx});
                    Agent.LOG.finest(msg);
                }

            }
        }
    }

    private boolean addAsyncTransaction(Transaction tx) {
        return !this.asyncTransactions.contains(tx) && this.asyncTransactions.add(tx);
    }

    public void asyncTransactionFinished(TransactionActivity txa) {
        if (!this.isComplete()) {
            if (this.transactionActivityRef.get() == txa) {
                Agent.LOG.fine("Cannot finish async transaction of itself: " + txa);
            } else {
                boolean rootIgnored = !txa.getRootTracer().isTransactionSegment();
                boolean noTracers = txa.getTracers().isEmpty();
                TransactionState transactionState = txa.getTransaction().getTransactionState();
                boolean noChildren = transactionState instanceof AsyncTransactionState
                                             ? ((AsyncTransactionState) transactionState)
                                                       .asyncTransactionActivitiesComplete
                                                       .isEmpty() : true;
                String msg;
                if (rootIgnored && noTracers && noChildren) {
                    if (this.asyncTransactions.remove(txa.getTransaction())) {
                        this.mergeAsyncTransactionData(txa);
                        if (Agent.LOG.isFinestEnabled()) {
                            msg = MessageFormat
                                          .format("Async transaction (excluded from trace) finished for {0}: {1} "
                                                          + "(remaining: {2})",
                                                         new Object[] {this.transactionActivityRef.get(), txa,
                                                                              this.asyncTransactions});
                            Agent.LOG.finest(msg);
                        }

                        while (this.asyncTransactions.remove(txa)) {
                            Agent.LOG.log(Level.FINEST, "Removed the transaction again: " + txa);
                        }
                    }
                } else if (this.asyncTransactions.remove(txa.getTransaction())
                                   && this.asyncTransactionActivitiesComplete.add(txa)) {
                    if (Agent.LOG.isFinestEnabled()) {
                        msg = MessageFormat.format("Async transaction finished for {0}: {1} (remaining: {2})",
                                                          new Object[] {this.transactionActivityRef.get(), txa,
                                                                               this.asyncTransactions});
                        Agent.LOG.finest(msg);
                    }

                    while (this.asyncTransactions.remove(txa)) {
                        Agent.LOG.log(Level.FINEST, "Removed the transaction again: " + txa);
                    }
                }

                this.tryComplete(true);
            }
        }
    }

    public void asyncJobStarted(TransactionHolder job) {
        if (!this.isComplete() && !this.invalidateAsyncJobs.get()) {
            if (!this.asyncJobs.contains(job) && this.asyncJobs.add(job) && Agent.LOG.isFinestEnabled()) {
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                String msg = MessageFormat.format("Async job started for {0}: {1} ({2})",
                                                         new Object[] {this.transactionActivityRef.get(), job,
                                                                              Arrays.asList(stackTrace).toString()});
                Agent.LOG.finest(msg);
            }

        }
    }

    public void asyncJobInvalidate(TransactionHolder job) {
        if (!this.isComplete()) {
            if (this.asyncJobs.remove(job)) {
                if (Agent.LOG.isFinestEnabled()) {
                    String msg = MessageFormat.format("Async job removed from transaction. {0}: {1} (remaining: {2})",
                                                             new Object[] {this.transactionActivityRef.get(), job,
                                                                                  this.asyncJobs});
                    Agent.LOG.finest(msg);
                }

                this.tryComplete(false);
            }

        }
    }

    public void asyncJobFinished(TransactionHolder job) {
        if (!this.isComplete()) {
            if (this.asyncJobs.remove(job)) {
                if (Agent.LOG.isFinestEnabled()) {
                    String msg = MessageFormat.format("Async job finished for {0}: {1} (remaining: {2})",
                                                             new Object[] {this.transactionActivityRef.get(), job,
                                                                                  this.asyncJobs});
                    Agent.LOG.finest(msg);
                }

                this.tryComplete(job != TransactionHolderDispatcherPointCut.TRANSACTION_HOLDER);
            }

        }
    }

    public void setInvalidateAsyncJobs(boolean invalidate) {
        this.invalidateAsyncJobs.set(invalidate);
    }

    public void printIncompleteTransactionGraph() {
        if (Agent.LOG.isFinerEnabled()) {
            Agent.LOG.finer("Job Graph for " + this.transactionActivityRef.get());
            this.printIncompleteTransactionGraph(0);
        }

    }

    private void printIncompleteTransactionGraph(int indentLevel) {
        if (indentLevel >= 150) {
            Agent.LOG.finer("Async nesting too deep!");
        } else {
            String indent = Strings.repeat("-", indentLevel * 2);
            Iterator i$ = this.asyncTransactions.iterator();

            while (i$.hasNext()) {
                Transaction th = (Transaction) i$.next();
                Agent.LOG.finer(indent + "+ Tx: " + th + (Transaction.getTransaction() == th ? " <!>" : ""));
                if (th.getTransactionState() instanceof AsyncTransactionState) {
                    AsyncTransactionState transaction = (AsyncTransactionState) th.getTransactionState();
                    transaction.printIncompleteTransactionGraph(indentLevel + 1);
                }
            }

            i$ = this.asyncJobs.iterator();

            while (i$.hasNext()) {
                TransactionHolder th1 = (TransactionHolder) i$.next();
                if (th1._nr_getTransaction() instanceof Transaction) {
                    Transaction transaction1 = (Transaction) th1._nr_getTransaction();
                    Agent.LOG.finer(indent + "> Job: " + th1 + " (" + transaction1 + ")");
                } else {
                    Agent.LOG.finer(indent + "> Th: " + th1);
                }
            }

        }
    }
}
