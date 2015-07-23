package com.newrelic.agent.stats;

import org.json.simple.JSONStreamAware;

public abstract interface StatsBase extends Cloneable, JSONStreamAware {
    public abstract boolean hasData();

    public abstract void reset();

    public abstract void merge(StatsBase paramStatsBase);

    public abstract Object clone() throws CloneNotSupportedException;
}