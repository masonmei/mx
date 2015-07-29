package com.newrelic.agent.extension.util;

public class XmlException extends Exception {
    private static final long serialVersionUID = 8308599094191068541L;

    public XmlException(String message, Throwable cause) {
        super(message, cause);
    }

    public XmlException(String message) {
        super(message);
    }
}