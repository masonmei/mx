package com.newrelic.agent.bridge;

public class NoOpAsyncApi implements AsyncApi {
    public void errorAsync(Object context, Throwable t) {
    }

    public void suspendAsync(Object asyncContext) {
    }

    public Transaction resumeAsync(Object asyncContext) {
        return null;
    }

    public void completeAsync(Object asyncContext) {
    }

    public void finishRootTracer() {
    }
}