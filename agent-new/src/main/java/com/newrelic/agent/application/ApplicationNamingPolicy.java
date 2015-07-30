package com.newrelic.agent.application;

import com.newrelic.agent.Transaction;
import com.newrelic.api.agent.ApplicationNamePriority;

public interface ApplicationNamingPolicy {
    boolean canSetApplicationName(Transaction paramTransaction, ApplicationNamePriority paramApplicationNamePriority);
}