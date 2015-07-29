package com.newrelic.agent.extension.jaxb;

public class UnmarshalException extends Exception {
    private static final long serialVersionUID = -3785749805564625068L;

    public UnmarshalException(Exception ex) {
        super(ex);
    }

    public UnmarshalException(String message) {
        super(message);
    }
}