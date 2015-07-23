package com.newrelic.agent.transaction;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;

class HigherPriorityTransactionNamingPolicy extends TransactionNamingPolicy {
    public boolean canSetTransactionName(Transaction tx, TransactionNamePriority priority) {
        if (priority == null) {
            return false;
        }
        PriorityTransactionName ptn = tx.getPriorityTransactionName();
        return priority.compareTo(ptn.getPriority()) > 0;
    }
}