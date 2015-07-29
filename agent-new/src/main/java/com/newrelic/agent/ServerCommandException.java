package com.newrelic.agent;

public class ServerCommandException extends Exception {
    private static final long serialVersionUID = 7001395828662633469L;

    public ServerCommandException(String message) {
        super(message);
    }
}