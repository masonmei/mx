package com.newrelic.agent.tracers;

import com.newrelic.agent.dispatchers.Dispatcher;

public abstract interface TransactionActivityInitiator {
    public abstract Dispatcher createDispatcher();
}