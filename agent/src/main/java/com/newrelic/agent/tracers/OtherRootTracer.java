package com.newrelic.agent.tracers;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.TransactionErrorPriority;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.dispatchers.OtherDispatcher;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;

public class OtherRootTracer extends DefaultTracer implements TransactionActivityInitiator {
    private final MetricNameFormat uri;

    public OtherRootTracer(Transaction transaction, ClassMethodSignature sig, Object object, MetricNameFormat uri) {
        this(transaction.getTransactionActivity(), sig, object, uri);
    }

    public OtherRootTracer(TransactionActivity activity, ClassMethodSignature sig, Object object,
                           MetricNameFormat uri) {
        super(activity, sig, object, new ClassMethodMetricNameFormat(sig, object), 6);
        this.uri = uri;
    }

    public Dispatcher createDispatcher() {
        return new OtherDispatcher(getTransaction(), uri);
    }

    protected void doFinish(Throwable throwable) {
        super.doFinish(throwable);
        if (equals(getTransaction().getTransactionActivity().getRootTracer())) {
            getTransaction().setThrowable(throwable, TransactionErrorPriority.TRACER);
        }
    }
}