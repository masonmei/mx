package com.newrelic.agent.bridge;

import java.lang.reflect.InvocationHandler;

public interface ExitTracer extends InvocationHandler, TracedMethod {
    void finish(int paramInt, Object paramObject);

    void finish(Throwable paramThrowable);
}