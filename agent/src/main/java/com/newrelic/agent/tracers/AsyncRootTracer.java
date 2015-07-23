package com.newrelic.agent.tracers;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.dispatchers.AsyncDispatcher;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;

public class AsyncRootTracer extends DefaultTracer implements TransactionActivityInitiator {
    private final MetricNameFormat uri;

    public AsyncRootTracer(Transaction transaction, ClassMethodSignature sig, Object object, MetricNameFormat uri) {
        super(transaction, sig, object, uri);
        this.uri = uri;
    }

    public Dispatcher createDispatcher() {
        return new AsyncDispatcher(getTransaction(), uri);
    }
}