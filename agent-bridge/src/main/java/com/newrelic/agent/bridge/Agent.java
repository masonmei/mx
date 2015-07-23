package com.newrelic.agent.bridge;

public interface Agent extends com.newrelic.api.agent.Agent {
    TracedMethod getTracedMethod();

    Transaction getTransaction();
}