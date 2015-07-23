package com.newrelic.agent.bridge;

import java.util.concurrent.TimeUnit;

import com.newrelic.api.agent.MetricAggregator;

public class NoOpMetricAggregator implements MetricAggregator {
    public static final MetricAggregator INSTANCE = new NoOpMetricAggregator();

    public void recordResponseTimeMetric(String name, long totalTime, long exclusiveTime, TimeUnit timeUnit) {
    }

    public void recordMetric(String name, float value) {
    }

    public void recordResponseTimeMetric(String name, long millis) {
    }

    public void incrementCounter(String name) {
    }

    public void incrementCounter(String name, int count) {
    }
}