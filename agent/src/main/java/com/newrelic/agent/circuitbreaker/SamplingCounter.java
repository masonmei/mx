package com.newrelic.agent.circuitbreaker;

public class SamplingCounter {
    private final long samplingRate;
    private long count;

    public SamplingCounter(long samplingRate) {
        count = 0L;
        this.samplingRate = samplingRate;
    }

    public boolean shouldSample() {
        if (++count > samplingRate) {
            count = 0L;
            return true;
        }
        return false;
    }
}