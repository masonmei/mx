package com.newrelic.agent;

import com.newrelic.agent.stats.TransactionStats;

public interface TransactionListener {
    void dispatcherTransactionFinished(TransactionData paramTransactionData, TransactionStats paramTransactionStats);
}