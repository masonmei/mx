package com.newrelic.agent.trace;

import com.newrelic.agent.TransactionData;
import java.util.List;

public abstract interface ITransactionSampler
{
  public abstract boolean noticeTransaction(TransactionData paramTransactionData);

  public abstract List<TransactionTrace> harvest(String paramString);

  public abstract void stop();

  public abstract long getMaxDurationInNanos();
}