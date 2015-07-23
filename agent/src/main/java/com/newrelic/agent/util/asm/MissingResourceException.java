package com.newrelic.agent.util.asm;

import java.io.IOException;

public class MissingResourceException extends IOException {
    private static final long serialVersionUID = 1177827391206078775L;

    public MissingResourceException(String message) {
        super(message);
    }
}