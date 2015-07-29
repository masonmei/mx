package com.newrelic.agent.sql;

import com.newrelic.agent.TransactionData;
import java.util.List;

public abstract interface SqlTracerAggregator
{
  public abstract List<SqlTrace> getAndClearSqlTracers();

  public abstract void addSqlTracers(TransactionData paramTransactionData);
}