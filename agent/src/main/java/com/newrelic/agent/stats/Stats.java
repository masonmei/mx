package com.newrelic.agent.stats;

public interface Stats extends CountStats {
    void recordDataPoint(float paramFloat);
}