package com.newrelic.agent.transaction;

public class UnmodifiableTransactionNameException extends Exception {
    private static final long serialVersionUID = 2277591207140681026L;

    public UnmodifiableTransactionNameException(Exception ex) {
        super(ex);
    }
}