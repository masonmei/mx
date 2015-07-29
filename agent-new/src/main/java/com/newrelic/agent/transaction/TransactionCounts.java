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
        this.maxSegments = config.getTransactionTracerConfig().getMaxSegments();
        this.maxTransactionSize = config.getTransactionSizeLimit();
    }

    public void incrementSize(int size) {
        this.transactionSize.addAndGet(size);
    }

    public int getTransactionSize() {
        return this.transactionSize.intValue();
    }

    public void addTracer() {
        int count = this.segmentCount.incrementAndGet();
        this.transactionSize.addAndGet(128);
        this.overSegmentLimit = (count > this.maxSegments);
    }

    public boolean isOverTracerSegmentLimit() {
        return this.overSegmentLimit;
    }

    public int getSegmentCount() {
        return this.segmentCount.get();
    }

    public boolean isOverTransactionSize() {
        return this.transactionSize.intValue() > this.maxTransactionSize;
    }

    public boolean shouldGenerateTransactionSegment() {
        return (!isOverTracerSegmentLimit()) && (!isOverTransactionSize());
    }

    public void incrementStackTraceCount() {
        this.stackTraceCount.incrementAndGet();
    }

    public int getStackTraceCount() {
        return this.stackTraceCount.intValue();
    }

    public int getExplainPlanCount() {
        return this.explainPlanCount.intValue();
    }

    public void incrementExplainPlanCountAndLogIfReachedMax(int max) {
        int updatedVal = this.explainPlanCount.incrementAndGet();
        if (updatedVal == max) {
            Agent.LOG.log(Level.FINER, "Reached the maximum number of explain plans.");
        }
    }
}