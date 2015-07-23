package com.newrelic.agent.tracers;

public abstract interface ISqlStatementTracer {
    public abstract Object getSql();

    public abstract void setExplainPlan(Object[] paramArrayOfObject);
}