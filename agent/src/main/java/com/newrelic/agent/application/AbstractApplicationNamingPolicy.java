package com.newrelic.agent.application;

import com.newrelic.agent.Transaction;
import com.newrelic.api.agent.ApplicationNamePriority;

public abstract class AbstractApplicationNamingPolicy implements ApplicationNamingPolicy {
    public abstract boolean canSetApplicationName(Transaction paramTransaction,
                                                  ApplicationNamePriority paramApplicationNamePriority);
}