package com.newrelic.agent.bridge;

import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;

class NoOpAgent implements Agent {
    static final Agent INSTANCE = new NoOpAgent();

    public Logger getLogger() {
        return NoOpLogger.INSTANCE;
    }

    public Config getConfig() {
        return NoOpConfig.Instance;
    }

    public TracedMethod getTracedMethod() {
        return NoOpTracedMethod.INSTANCE;
    }

    public Transaction getTransaction() {
        return NoOpTransaction.INSTANCE;
    }

    public MetricAggregator getMetricAggregator() {
        return NoOpMetricAggregator.INSTANCE;
    }

    public Insights getInsights() {
        return NoOpInsights.INSTANCE;
    }
}