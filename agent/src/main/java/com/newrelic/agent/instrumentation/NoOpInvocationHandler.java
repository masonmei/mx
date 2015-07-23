package com.newrelic.agent.instrumentation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

class NoOpInvocationHandler implements InvocationHandler {
    static final InvocationHandler INVOCATION_HANDLER = new NoOpInvocationHandler();

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
    }
}