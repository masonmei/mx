package com.newrelic.agent.dispatchers;

import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;

public abstract interface Dispatcher {
    public abstract void setTransactionName();

    public abstract String getUri();

    public abstract TransactionTracerConfig getTransactionTracerConfig();

    public abstract boolean isWebTransaction();

    public abstract boolean isAsyncTransaction();

    public abstract void transactionFinished(String paramString, TransactionStats paramTransactionStats);

    public abstract String getCookieValue(String paramString);

    public abstract String getHeader(String paramString);

    public abstract Request getRequest();

    public abstract void setRequest(Request paramRequest);

    public abstract Response getResponse();

    public abstract void setResponse(Response paramResponse);

    public abstract boolean isIgnoreApdex();

    public abstract void setIgnoreApdex(boolean paramBoolean);
}