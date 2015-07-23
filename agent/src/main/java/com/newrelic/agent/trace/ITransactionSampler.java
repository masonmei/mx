package com.newrelic.agent.trace;

import java.util.List;

import com.newrelic.agent.TransactionData;

public abstract interface ITransactionSampler {
    public abstract boolean noticeTransaction(TransactionData paramTransactionData);

    public abstract List<TransactionTrace> harvest(String paramString);

    public abstract void stop();

    public abstract long getMaxDurationInNanos();
}