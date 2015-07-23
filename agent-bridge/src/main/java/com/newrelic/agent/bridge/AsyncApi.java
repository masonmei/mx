package com.newrelic.agent.bridge;

public interface AsyncApi {
    @Deprecated
    void errorAsync(Object paramObject, Throwable paramThrowable);

    @Deprecated
    void suspendAsync(Object paramObject);

    @Deprecated
    Transaction resumeAsync(Object paramObject);

    @Deprecated
    void completeAsync(Object paramObject);

    @Deprecated
    void finishRootTracer();
}