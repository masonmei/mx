package com.newrelic.agent;

import com.newrelic.agent.bridge.CrossProcessState;

public interface CrossProcessTransactionState extends CrossProcessState {
    void writeResponseHeaders();

    String getTripId();

    int generatePathHash();

    String getAlternatePathHashes();
}