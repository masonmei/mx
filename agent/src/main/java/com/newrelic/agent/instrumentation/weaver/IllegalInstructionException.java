package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.agent.util.AgentError;

class IllegalInstructionException extends AgentError {
    private static final long serialVersionUID = 4541357282999714780L;

    public IllegalInstructionException(String message) {
        super(message);
    }

    public IllegalInstructionException(String message, Error ex) {
        super(message, ex);
    }
}