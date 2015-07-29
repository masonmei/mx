package com.newrelic.agent;

import com.newrelic.agent.stats.TransactionStats;

public abstract interface TransactionListener {
    public abstract void dispatcherTransactionFinished(TransactionData paramTransactionData,
                                                       TransactionStats paramTransactionStats);
}