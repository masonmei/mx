package com.newrelic.agent.dispatchers;

import com.newrelic.agent.Transaction;

public abstract class DefaultDispatcher implements Dispatcher {
    private final Transaction transaction;
    private volatile boolean ignoreApdex = false;

    public DefaultDispatcher(Transaction transaction) {
        this.transaction = transaction;
    }

    public boolean isAsyncTransaction() {
        return false;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public boolean isIgnoreApdex() {
        return ignoreApdex;
    }

    public final void setIgnoreApdex(boolean ignore) {
        ignoreApdex = ignore;
    }

    protected String getTransTotalName(String transactionName, String rootMetricName) {
        if ((transactionName != null) && (transactionName.indexOf(rootMetricName) == 0)) {
            StringBuilder totalTimeName = new StringBuilder(rootMetricName.length() + rootMetricName.length());
            totalTimeName.append(rootMetricName);
            totalTimeName.append("TotalTime");
            totalTimeName.append(transactionName.substring(rootMetricName.length()));
            return totalTimeName.toString();
        }
        return null;
    }

    protected String getApdexMetricName(String blameMetricName, String rootMetricName, String apdexMetricName) {
        if ((blameMetricName != null) && (blameMetricName.indexOf(rootMetricName) == 0)) {
            StringBuilder apdexName = new StringBuilder(apdexMetricName.length() + rootMetricName.length());
            apdexName.append(apdexMetricName);
            apdexName.append(blameMetricName.substring(rootMetricName.length()));
            return apdexName.toString();
        }
        return null;
    }
}