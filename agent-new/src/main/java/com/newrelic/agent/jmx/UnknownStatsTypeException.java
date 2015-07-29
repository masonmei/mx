package com.newrelic.agent.jmx;

class UnknownStatsTypeException extends Exception {
    public UnknownStatsTypeException(String msg) {
        super(msg);
    }
}