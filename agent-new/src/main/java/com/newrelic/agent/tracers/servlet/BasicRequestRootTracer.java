package com.newrelic.agent.tracers.servlet;

import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionErrorPriority;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.dispatchers.WebRequestDispatcher;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.SkipTracerException;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TransactionActivityInitiator;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;

public class BasicRequestRootTracer extends DefaultTracer implements TransactionActivityInitiator {
    private Request request;
    private Response response;

    public BasicRequestRootTracer(Transaction transaction, ClassMethodSignature sig, Object dispatcher, Request request,
                                  Response response) {
        this(transaction, sig, dispatcher, request, response, new SimpleMetricNameFormat("RequestDispatcher",
                                                                                                ClassMethodMetricNameFormat
                                                                                                        .getMetricName(sig,
                                                                                                                              dispatcher,
                                                                                                                              "RequestDispatcher")));

        this.request = request;
        this.response = response;
    }

    public BasicRequestRootTracer(Transaction transaction, ClassMethodSignature sig, Object dispatcher, Request request,
                                  Response response, MetricNameFormat metricNameFormatter) {
        super(transaction, sig, dispatcher, metricNameFormatter);

        this.request = request;
        this.response = response;

        Tracer rootTracer = transaction.getTransactionActivity().getRootTracer();
        if (rootTracer != null) {
            throw new SkipTracerException();
        }
    }

    public Dispatcher createDispatcher() {
        return new WebRequestDispatcher(this.request, this.response, getTransaction());
    }

    protected void reset() {
        super.reset();
    }

    protected final void doFinish(Throwable throwable) {
        try {
            super.doFinish(throwable);
            getTransaction().setThrowable(throwable, TransactionErrorPriority.TRACER);
        } catch (Exception e) {
            Agent.LOG
                    .log(Level.FINE, "An error occurred calling doFinish() for dispatcher tracer with an exception", e);
        }
    }
}