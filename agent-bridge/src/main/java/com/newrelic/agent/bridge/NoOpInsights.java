package com.newrelic.agent.bridge;

import java.util.Map;

import com.newrelic.api.agent.Insights;

class NoOpInsights implements Insights {
    static final Insights INSTANCE = new NoOpInsights();

    public void recordCustomEvent(String eventType, Map<String, Object> attributes) {
    }
}