package com.newrelic.agent;

import com.newrelic.agent.bridge.CrossProcessState;

public abstract interface CrossProcessTransactionState extends CrossProcessState {
    public abstract void writeResponseHeaders();

    public abstract String getTripId();

    public abstract int generatePathHash();

    public abstract String getAlternatePathHashes();
}