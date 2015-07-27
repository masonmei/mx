package com.newrelic.agent;

import java.util.logging.Level;

import com.newrelic.agent.instrumentation.pointcuts.TransactionHolder;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.SkipTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFactory;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.MetricNameFormats;

public class TransactionStateImpl implements TransactionState {
    public Tracer getTracer(Transaction tx, TracerFactory tracerFactory, ClassMethodSignature sig, Object obj,
                            Object[] args) {
        TransactionActivity activity = tx.getTransactionActivity();
        if ((tx.isIgnore()) || (activity.isTracerStartLocked())) {
            return null;
        }

        Tracer tracer = tracerFactory.getTracer(tx, sig, obj, args);
        return tracerStarted(tx, sig, tracer);
    }

    public Tracer getTracer(Transaction tx, String tracerFactoryName, ClassMethodSignature sig, Object obj,
                            Object[] args) {
        TracerFactory tracerFactory = ServiceFactory.getTracerService().getTracerFactory(tracerFactoryName);
        return getTracer(tx, tracerFactory, sig, obj, args);
    }

    public Tracer getTracer(Transaction tx, Object invocationTarget, ClassMethodSignature sig, String metricName,
                            int flags) {
        TransactionActivity activity = tx.getTransactionActivity();
        if ((tx.isIgnore()) || (activity.isTracerStartLocked())) {
            return null;
        }

        MetricNameFormat mnf = MetricNameFormats.getFormatter(invocationTarget, sig, metricName, flags);

        Tracer tracer;
        if (TracerFlags.isDispatcher(flags)) {
            tracer = new OtherRootTracer(tx, sig, invocationTarget, mnf);
        } else {
            tracer = new DefaultTracer(tx, sig, invocationTarget, mnf, flags);
        }
        return tracerStarted(tx, sig, tracer);
    }

    private Tracer tracerStarted(Transaction tx, ClassMethodSignature sig, Tracer tracer) {
        if ((tracer == null) || ((tracer instanceof SkipTracer))) {
            return tracer;
        }

        tracer = tx.getTransactionActivity().tracerStarted(tracer);

        if ((tracer != null) && (Agent.LOG.isLoggable(Level.FINER))) {
            if (tracer == tx.getRootTracer()) {
                Agent.LOG.log(Level.FINER, "Transaction started {0}", tx);
            }
            Agent.LOG.log(Level.FINER, "Tracer ({3}) Started: {0}.{1}{2}", sig.getClassName(), sig.getMethodName(), sig.getMethodDesc(),
                                 tracer);
        }

        return tracer;
    }

    public Tracer getRootTracer() {
        return null;
    }

    public void resume() {
    }

    public void suspend() {
    }

    public void complete() {
    }

    public boolean finish(Transaction tx, Tracer tracer) {
        return true;
    }

    public void suspendRootTracer() {
    }

    public void asyncJobStarted(TransactionHolder job) {
    }

    public void asyncJobFinished(TransactionHolder job) {
    }

    public void asyncTransactionStarted(Transaction tx, TransactionHolder txHolder) {
    }

    public void asyncTransactionFinished(TransactionActivity txa) {
    }

    public void mergeAsyncTracers() {
    }

    public void asyncJobInvalidate(TransactionHolder job) {
    }

    public void setInvalidateAsyncJobs(boolean invalidate) {
    }
}