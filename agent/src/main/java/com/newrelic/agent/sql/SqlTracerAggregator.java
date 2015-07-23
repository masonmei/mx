package com.newrelic.agent.sql;

import java.util.List;

import com.newrelic.agent.TransactionData;

public abstract interface SqlTracerAggregator {
    public abstract List<SqlTrace> getAndClearSqlTracers();

    public abstract void addSqlTracers(TransactionData paramTransactionData);
}