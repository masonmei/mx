package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.agent.util.AgentError;

class InvalidReferenceException extends AgentError {
    private static final long serialVersionUID = -6074715187913414583L;

    public InvalidReferenceException(String message) {
        super(message);
    }
}