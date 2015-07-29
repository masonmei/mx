package com.newrelic.agent.dispatchers;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.transaction.OtherTransactionNamer;
import com.newrelic.agent.transaction.TransactionNamer;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;

public class AsyncDispatcher extends DefaultDispatcher {
    private final MetricNameFormat uri;

    public AsyncDispatcher(Transaction transaction, MetricNameFormat uri) {
        super(transaction);
        this.uri = uri;
    }

    public Request getRequest() {
        return null;
    }

    public void setRequest(Request request) {
    }

    public String getUri() {
        return this.uri.getMetricName();
    }

    public void setTransactionName() {
        TransactionNamer tn = OtherTransactionNamer.create(getTransaction(), getUri());
        tn.setTransactionName();
    }

    public TransactionTracerConfig getTransactionTracerConfig() {
        return getTransaction().getRootTransaction().getAgentConfig().getRequestTransactionTracerConfig();
    }

    public boolean isWebTransaction() {
        return true;
    }

    public boolean isAsyncTransaction() {
        return true;
    }

    public void transactionFinished(String transactionName, TransactionStats stats) {
    }

    public String getCookieValue(String name) {
        return null;
    }

    public String getHeader(String name) {
        return null;
    }

    public Response getResponse() {
        return null;
    }

    public void setResponse(Response response) {
    }
}