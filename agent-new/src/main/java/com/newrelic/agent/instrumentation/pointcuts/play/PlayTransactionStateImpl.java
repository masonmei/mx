//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts.play;

import java.util.ArrayDeque;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.TransactionStateImpl;
import com.newrelic.agent.instrumentation.pointcuts.play.PlayDispatcherPointCut.PlayHttpRequest;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.MethodExitTracerNoSkip;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFactory;

public class PlayTransactionStateImpl extends TransactionStateImpl {
    private static final TransactionActivity NULL_TRANSACTION_ACTIVITY = null;
    private static final Tracer NULL_TRACER;

    static {
        NULL_TRACER = new MethodExitTracerNoSkip((ClassMethodSignature) null, NULL_TRANSACTION_ACTIVITY) {
            protected void doFinish(int opcode, Object returnValue) {
            }
        };
    }

    private final PlayHttpRequest request;
    private final ArrayDeque<Tracer> tracers;
    private PlayTransactionStateImpl.State state;
    private ArrayDeque<Tracer> suspendedTracers;

    public PlayTransactionStateImpl(PlayHttpRequest request) {
        this.state = PlayTransactionStateImpl.State.RUNNING;
        this.tracers = new ArrayDeque();
        this.suspendedTracers = new ArrayDeque();
        this.request = request;
    }

    public Tracer getTracer(Transaction tx, TracerFactory tracerFactory, ClassMethodSignature signature, Object object,
                            Object... args) {
        Tracer tracer;
        if (this.state == PlayTransactionStateImpl.State.RESUMING) {
            tracer = this.removeSuspendedTracer();
            if (tracer == NULL_TRACER) {
                return null;
            }

            if (tracer != null) {
                return tracer;
            }

            this.state = PlayTransactionStateImpl.State.RUNNING;
        }

        tracer = super.getTracer(tx, tracerFactory, signature, object, args);
        this.addTracer(tracer == null ? NULL_TRACER : tracer);
        return tracer;
    }

    public Tracer getTracer(Transaction tx, Object invocationTarget, ClassMethodSignature sig, String metricName,
                            int flags) {
        Tracer tracer;
        if (this.state == PlayTransactionStateImpl.State.RESUMING) {
            tracer = this.removeSuspendedTracer();
            if (tracer == NULL_TRACER) {
                return null;
            }

            if (tracer != null) {
                return tracer;
            }

            this.state = PlayTransactionStateImpl.State.RUNNING;
        }

        tracer = super.getTracer(tx, invocationTarget, sig, metricName, flags);
        this.addTracer(tracer == null ? NULL_TRACER : tracer);
        return tracer;
    }

    private Tracer removeSuspendedTracer() {
        return (Tracer) this.suspendedTracers.pollFirst();
    }

    private void removeTracer(Tracer tracer) {
        Tracer lastTracer;
        for (lastTracer = (Tracer) this.tracers.peekLast(); lastTracer == NULL_TRACER;
             lastTracer = (Tracer) this.tracers.peekLast()) {
            this.tracers.pollLast();
        }

        if (lastTracer == tracer) {
            this.tracers.pollLast();
        }

    }

    private void addTracer(Tracer tracer) {
        this.tracers.addLast(tracer);
    }

    public void resume() {
        this.state = PlayTransactionStateImpl.State.RESUMING;
    }

    public void suspend() {
        this.state = PlayTransactionStateImpl.State.SUSPENDING;
    }

    public void suspendRootTracer() {
        this.state = PlayTransactionStateImpl.State.SUSPENDING_ROOT_TRACER;
    }

    public boolean finish(Transaction tx, Tracer tracer) {
        if (this.state == PlayTransactionStateImpl.State.SUSPENDING) {
            if (tracer == tx.getRootTracer()) {
                this.saveTransaction(tx);
            }

            return false;
        } else if (this.state == PlayTransactionStateImpl.State.SUSPENDING_ROOT_TRACER
                           && tracer == tx.getRootTracer()) {
            this.saveTransaction(tx);
            return false;
        } else {
            if (this.state == PlayTransactionStateImpl.State.RESUMING) {
                this.suspendedTracers.clear();
                this.state = PlayTransactionStateImpl.State.RUNNING;
            }

            this.removeTracer(tracer);
            return true;
        }
    }

    private void saveTransaction(Transaction tx) {
        this.suspendedTracers = new ArrayDeque(this.tracers);
        this.request._nr_setTransaction(tx);
        Transaction.clearTransaction();
    }

    private static enum State {
        RESUMING,
        RUNNING,
        SUSPENDING,
        SUSPENDING_ROOT_TRACER;

        private State() {
        }
    }
}
