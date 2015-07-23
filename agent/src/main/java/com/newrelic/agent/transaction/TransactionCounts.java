package com.newrelic.agent.transaction;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;

public class TransactionCounts {
    private static final int APPROX_TRACER_SIZE = 128;
    private final int maxTransactionSize;
    private final int maxSegments;
    private final AtomicInteger transactionSize = new AtomicInteger(0);
    private final AtomicInteger segmentCount = new AtomicInteger(0);
    private final AtomicInteger explainPlanCount = new AtomicInteger(0);
    private final AtomicInteger stackTraceCount = new AtomicInteger(0);
    private volatile boolean overSegmentLimit;

    public TransactionCounts(AgentConfig config) {
        maxSegments = config.getTransactionTracerConfig().getMaxSegments();
        maxTransactionSize = config.getTransactionSizeLimit();
    }

    public void incrementSize(int size) {
        transactionSize.addAndGet(size);
    }

    public int getTransactionSize() {
        return transactionSize.intValue();
    }

    public void addTracer() {
        int count = segmentCount.incrementAndGet();
        transactionSize.addAndGet(128);
        overSegmentLimit = (count > maxSegments);
    }

    public boolean isOverTracerSegmentLimit() {
        return overSegmentLimit;
    }

    public int getSegmentCount() {
        return segmentCount.get();
    }

    public boolean isOverTransactionSize() {
        return transactionSize.intValue() > maxTransactionSize;
    }

    public boolean shouldGenerateTransactionSegment() {
        return (!isOverTracerSegmentLimit()) && (!isOverTransactionSize());
    }

    public void incrementStackTraceCount() {
        stackTraceCount.incrementAndGet();
    }

    public int getStackTraceCount() {
        return stackTraceCount.intValue();
    }

    public int getExplainPlanCount() {
        return explainPlanCount.intValue();
    }

    public void incrementExplainPlanCountAndLogIfReachedMax(int max) {
        int updatedVal = explainPlanCount.incrementAndGet();
        if (updatedVal == max) {
            Agent.LOG.log(Level.FINER, "Reached the maximum number of explain plans.");
        }
    }
}