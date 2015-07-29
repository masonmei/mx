package com.newrelic.agent.transaction;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;

public abstract class AbstractTransactionNamer
  implements TransactionNamer
{
  private final Transaction tx;
  private final String uri;

  protected AbstractTransactionNamer(Transaction tx, String uri)
  {
    this.tx = tx;
    this.uri = uri;
  }

  protected final String getUri() {
    return this.uri;
  }

  protected final Transaction getTransaction() {
    return this.tx;
  }

  protected boolean canSetTransactionName() {
    return canSetTransactionName(TransactionNamePriority.REQUEST_URI);
  }

  protected boolean canSetTransactionName(TransactionNamePriority priority) {
    if ((this.tx == null) || (this.tx.isIgnore())) {
      return false;
    }
    TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
    return policy.canSetTransactionName(this.tx, priority);
  }

  protected void setTransactionName(String name, String category, TransactionNamePriority priority) {
    if (canSetTransactionName(priority)) {
      TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
      policy.setTransactionName(this.tx, name, category, priority);
    }
  }
}