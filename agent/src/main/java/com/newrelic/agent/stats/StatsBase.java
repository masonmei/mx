package com.newrelic.agent.stats;

import com.newrelic.deps.org.json.simple.JSONStreamAware;

public interface StatsBase extends Cloneable, JSONStreamAware {
    boolean hasData();

    void reset();

    void merge(StatsBase paramStatsBase);

    Object clone() throws CloneNotSupportedException;
}