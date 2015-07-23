package com.newrelic.agent.tracers.servlet;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicReference;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionStateImpl;
import com.newrelic.agent.tracers.AbstractTracer;
import com.newrelic.agent.tracers.AbstractTracerFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFactory;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

public class ServletAsyncTransactionStateImpl extends TransactionStateImpl {
    private static final ClassMethodSignature ASYNC_PROCESSING_SIG =
            new ClassMethodSignature("NR_RECORD_ASYNC_PROCESSING_CLASS", "NR_RECORD_ASYNC_PROCESSING_METHOD", "()V");

    private static final MetricNameFormat ASYNC_PROCESSING_FORMAT = new SimpleMetricNameFormat("AsyncProcessing");
    private static final TracerFactory ASYNC_TRACER_FACTORY = new AsyncTracerFactory();
    private final Transaction transaction;
    private final AtomicReference<State> state = new AtomicReference(State.RUNNING);
    private volatile Tracer rootTracer;
    private volatile AbstractTracer asyncProcessingTracer;

    public ServletAsyncTransactionStateImpl(Transaction tx) {
        transaction = tx;
    }

    public Tracer getTracer(Transaction tx, TracerFactory tracerFactory, ClassMethodSignature signature, Object object,
                            Object[] args) {
        if (state.compareAndSet(State.RESUMING, State.RUNNING)) {
            Tracer tracer = resumeRootTracer();
            if (tracer != null) {
                return tracer;
            }
        }
        return super.getTracer(tx, tracerFactory, signature, object, args);
    }

    public Tracer getRootTracer() {
        if (state.compareAndSet(State.RESUMING, State.RUNNING)) {
            return resumeRootTracer();
        }
        return null;
    }

    public void resume() {
        if (!state.compareAndSet(State.SUSPENDING, State.RESUMING)) {
            return;
        }
        if (Agent.LOG.isFinerEnabled()) {
            Agent.LOG.finer(MessageFormat.format("Resuming transaction {0}", new Object[] {transaction}));
        }
        Transaction.clearTransaction();
        Transaction.setTransaction(transaction);
    }

    public void suspendRootTracer() {
        Transaction currentTx = Transaction.getTransaction();
        if (transaction != currentTx) {
            if (Agent.LOG.isFinerEnabled()) {
                Agent.LOG.finer(MessageFormat.format("Unable to suspend transaction {0} because it is not the current "
                                                             + "transaction {1}",
                                                            new Object[] {transaction, currentTx}));
            }

            return;
        }
        if (!state.compareAndSet(State.RUNNING, State.SUSPENDING)) {
            return;
        }
        if (Agent.LOG.isFinerEnabled()) {
            Agent.LOG.finer(MessageFormat.format("Transaction {0} is suspended", new Object[] {transaction}));
        }
    }

    public void complete() {
        if (!state.compareAndSet(State.SUSPENDING, State.RUNNING)) {
            return;
        }
        if (Agent.LOG.isFinerEnabled()) {
            Agent.LOG.finer(MessageFormat.format("Completing transaction {0}", new Object[] {transaction}));
        }
        Transaction currentTx = Transaction.getTransaction();
        if (currentTx != transaction) {
            Transaction.clearTransaction();
            Transaction.setTransaction(transaction);
        }
        try {
            Tracer tracer = resumeRootTracer();
            if (tracer != null) {
                tracer.finish(176, null);
            }
        } finally {
            if (currentTx != transaction) {
                Transaction.clearTransaction();
                Transaction.setTransaction(currentTx);
            }
        }
    }

    public boolean finish(Transaction tx, Tracer tracer) {
        if ((state.get() == State.SUSPENDING) && (tracer == tx.getRootTracer())) {
            suspendRootTracer(tx, tx.getRootTracer());
            return false;
        }
        return true;
    }

    private void suspendRootTracer(Transaction tx, Tracer tracer) {
        rootTracer = tracer;
        startAsyncProcessingTracer(tx);
        Transaction.clearTransaction();
    }

    private void startAsyncProcessingTracer(Transaction tx) {
        if (asyncProcessingTracer == null) {
            asyncProcessingTracer =
                    ((AbstractTracer) super.getTracer(tx, ASYNC_TRACER_FACTORY, ASYNC_PROCESSING_SIG, null,
                                                             (Object[]) null));
        }
    }

    private Tracer resumeRootTracer() {
        stopAsyncProcessingTracer();
        Tracer tracer = rootTracer;
        rootTracer = null;
        return tracer;
    }

    private void stopAsyncProcessingTracer() {
        if (asyncProcessingTracer != null) {
            asyncProcessingTracer.finish(176, null);
        }
        asyncProcessingTracer = null;
    }

    private static enum State {
        RESUMING, RUNNING, SUSPENDING;
    }

    private static class AsyncTracerFactory extends AbstractTracerFactory {
        public Tracer doGetTracer(Transaction tx, ClassMethodSignature sig, Object object, Object[] args) {
            return new DefaultTracer(tx, sig, object, ServletAsyncTransactionStateImpl.ASYNC_PROCESSING_FORMAT);
        }
    }
}