package com.newrelic.agent.instrumentation;

public class StopProcessingException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public StopProcessingException(String msg) {
        super(msg);
    }
}