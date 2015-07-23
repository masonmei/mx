package com.newrelic.agent.trace;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.service.ServiceFactory;

public class RandomTransactionSampler implements ITransactionSampler {
    private static final TransactionData FINISHED = new TransactionData(null, 0);
    private final int maxTraces;
    private final AtomicReference<TransactionData> expensiveTransaction = new AtomicReference();
    private int tracesSent;

    protected RandomTransactionSampler(int maxTraces) {
        this.maxTraces = maxTraces;
    }

    public static RandomTransactionSampler startSampler(int maxTraces) {
        RandomTransactionSampler transactionSampler = new RandomTransactionSampler(maxTraces);
        transactionSampler.start();
        return transactionSampler;
    }

    public boolean noticeTransaction(TransactionData td) {
        if (expensiveTransaction.compareAndSet(null, td)) {
            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = MessageFormat.format("Captured random transaction trace for {0} {1}",
                                                         new Object[] {td.getApplicationName(), td});

                Agent.LOG.finer(msg);
            }
            return true;
        }
        return false;
    }

    public List<TransactionTrace> harvest(String appName) {
        TransactionData td = (TransactionData) expensiveTransaction.get();
        if (td == FINISHED) {
            return Collections.emptyList();
        }
        if (td == null) {
            return Collections.emptyList();
        }
        if (td.getApplicationName() != appName) {
            return Collections.emptyList();
        }
        if (shouldFinish()) {
            td = (TransactionData) expensiveTransaction.getAndSet(FINISHED);
            stop();
        } else {
            td = (TransactionData) expensiveTransaction.getAndSet(null);
        }
        tracesSent += 1;
        return getTransactionTrace(td);
    }

    private List<TransactionTrace> getTransactionTrace(TransactionData td) {
        TransactionTrace trace = TransactionTrace.getTransactionTrace(td);
        if (Agent.LOG.isLoggable(Level.FINER)) {
            String msg = MessageFormat.format("Sending random transaction trace for {0}: {1}",
                                                     new Object[] {td.getApplicationName(), td});

            Agent.LOG.finer(msg);
        }
        List traces = new ArrayList(1);
        traces.add(trace);
        return traces;
    }

    private boolean shouldFinish() {
        return tracesSent >= maxTraces;
    }

    public void stop() {
        ServiceFactory.getTransactionTraceService().removeTransactionTraceSampler(this);
        if (Agent.LOG.isLoggable(Level.FINER)) {
            String msg = MessageFormat.format("Stopped random transaction tracing: max traces={1}",
                                                     new Object[] {Integer.valueOf(maxTraces)});
            Agent.LOG.finer(msg);
        }
    }

    private void start() {
        ServiceFactory.getTransactionTraceService().addTransactionTraceSampler(this);
        if (Agent.LOG.isLoggable(Level.FINER)) {
            String msg = MessageFormat.format("Started random transaction tracing: max traces={1}",
                                                     new Object[] {Integer.valueOf(maxTraces)});
            Agent.LOG.finer(msg);
        }
    }

    public long getMaxDurationInNanos() {
        return 9223372036854775807L;
    }
}