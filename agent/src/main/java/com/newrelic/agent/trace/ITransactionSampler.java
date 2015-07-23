package com.newrelic.agent.trace;

import java.util.List;

import com.newrelic.agent.TransactionData;

public interface ITransactionSampler {
    boolean noticeTransaction(TransactionData paramTransactionData);

    List<TransactionTrace> harvest(String paramString);

    void stop();

    long getMaxDurationInNanos();
}