package com.newrelic.agent.stats;

public enum ApdexPerfZone {
    SATISFYING("S"), TOLERATING("T"), FRUSTRATING("F");

    private final String z;

    private ApdexPerfZone(String z) {
        this.z = z;
    }

    public static ApdexPerfZone getZone(long responseTimeMillis, long apdexTInMillis) {
        if (responseTimeMillis <= apdexTInMillis) {
            return SATISFYING;
        }
        if (responseTimeMillis <= 4L * apdexTInMillis) {
            return TOLERATING;
        }
        return FRUSTRATING;
    }

    public String getZone() {
        return this.z;
    }
}