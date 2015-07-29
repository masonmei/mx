package com.newrelic.agent.transaction;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;

public class OtherTransactionNamer extends AbstractTransactionNamer
{
  private OtherTransactionNamer(Transaction tx, String dispatcherUri)
  {
    super(tx, dispatcherUri);
  }

  public void setTransactionName()
  {
    setTransactionName(getUri(), "", TransactionNamePriority.REQUEST_URI);
  }

  public static TransactionNamer create(Transaction tx, String dispatcherUri) {
    return new OtherTransactionNamer(tx, dispatcherUri);
  }
}