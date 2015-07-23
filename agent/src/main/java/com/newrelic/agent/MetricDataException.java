package com.newrelic.agent;

public class MetricDataException extends Exception {
    private static final long serialVersionUID = 1L;

    public MetricDataException(String message) {
        super(message);
    }
}