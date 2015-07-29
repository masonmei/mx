package com.newrelic.agent.transaction;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.util.Strings;

public abstract class TransactionNamingPolicy
{
  private static final HigherPriorityTransactionNamingPolicy HIGHER_PRIORITY_INSTANCE = new HigherPriorityTransactionNamingPolicy();
  private static final SameOrHigherPriorityTransactionNamingPolicy SAME_OR_HIGHER_INSTANCE = new SameOrHigherPriorityTransactionNamingPolicy();

  public final boolean setTransactionName(Transaction tx, String name, String category, TransactionNamePriority priority)
  {
    return tx.conditionalSetPriorityTransactionName(this, name, category, priority);
  }

  public abstract boolean canSetTransactionName(Transaction paramTransaction, TransactionNamePriority paramTransactionNamePriority);

  public PriorityTransactionName getPriorityTransactionName(Transaction tx, String name, String category, TransactionNamePriority priority)
  {
    if (category == null) {
      return PriorityTransactionName.create(name, category, priority);
    }
    if (name == null) {
      return PriorityTransactionName.create(name, category, priority);
    }
    String txType = tx.isWebTransaction() ? "WebTransaction" : "OtherTransaction";
    if (!Strings.isEmpty(name)) {
      if (name.startsWith(txType)) {
        return PriorityTransactionName.create(name, category, priority);
      }
      if (!name.startsWith("/")) {
        name = '/' + name;
      }
    }
    if (category.length() > 0) {
      name = '/' + category + name;
    }

    return PriorityTransactionName.create(tx, name, category, priority);
  }

  public static TransactionNamingPolicy getSameOrHigherPriorityTransactionNamingPolicy() {
    return SAME_OR_HIGHER_INSTANCE;
  }

  public static TransactionNamingPolicy getHigherPriorityTransactionNamingPolicy() {
    return HIGHER_PRIORITY_INSTANCE;
  }
}