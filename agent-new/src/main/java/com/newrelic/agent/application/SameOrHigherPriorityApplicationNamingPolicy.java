package com.newrelic.agent.application;

import com.newrelic.agent.Transaction;
import com.newrelic.api.agent.ApplicationNamePriority;

public class SameOrHigherPriorityApplicationNamingPolicy extends AbstractApplicationNamingPolicy
{
  private static final SameOrHigherPriorityApplicationNamingPolicy INSTANCE = new SameOrHigherPriorityApplicationNamingPolicy();

  public boolean canSetApplicationName(Transaction transaction, ApplicationNamePriority priority)
  {
    PriorityApplicationName pan = transaction.getPriorityApplicationName();
    return priority.compareTo(pan.getPriority()) >= 0;
  }

  public static SameOrHigherPriorityApplicationNamingPolicy getInstance() {
    return INSTANCE;
  }
}