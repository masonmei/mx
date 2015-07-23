package com.newrelic.agent.application;

import com.newrelic.agent.Transaction;
import com.newrelic.api.agent.ApplicationNamePriority;

public class HigherPriorityApplicationNamingPolicy extends AbstractApplicationNamingPolicy {
    private static final HigherPriorityApplicationNamingPolicy INSTANCE = new HigherPriorityApplicationNamingPolicy();

    public static HigherPriorityApplicationNamingPolicy getInstance() {
        return INSTANCE;
    }

    public boolean canSetApplicationName(Transaction transaction, ApplicationNamePriority priority) {
        PriorityApplicationName pan = transaction.getPriorityApplicationName();
        return priority.compareTo(pan.getPriority()) > 0;
    }
}