package com.newrelic.agent.tracers;

public abstract interface TimedItem {
    public abstract long getDurationInMilliseconds();

    public abstract long getDuration();

    public abstract long getExclusiveDuration();
}