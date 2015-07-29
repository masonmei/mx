package com.newrelic.agent.deadlock;

public class DeadlockException extends Exception {
    public DeadlockException(String message) {
        super(message);
    }
}