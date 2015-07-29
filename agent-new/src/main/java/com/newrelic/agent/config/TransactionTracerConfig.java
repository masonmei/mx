package com.newrelic.agent.config;

public abstract interface TransactionTracerConfig {
    public abstract boolean isEnabled();

    public abstract String getRecordSql();

    public abstract boolean isLogSql();

    public abstract int getInsertSqlMaxLength();

    public abstract long getTransactionThresholdInMillis();

    public abstract long getTransactionThresholdInNanos();

    public abstract double getStackTraceThresholdInMillis();

    public abstract double getStackTraceThresholdInNanos();

    public abstract double getExplainThresholdInMillis();

    public abstract double getExplainThresholdInNanos();

    public abstract boolean isExplainEnabled();

    public abstract int getMaxExplainPlans();

    public abstract boolean isGCTimeEnabled();

    public abstract int getMaxStackTraces();

    public abstract int getMaxSegments();

    public abstract int getTopN();

    public abstract boolean isStackBasedNamingEnabled();
}