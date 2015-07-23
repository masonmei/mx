package com.newrelic.agent.bridge;

public interface StatusCodePolicy {
    int nextStatus(int paramInt1, int paramInt2);
}