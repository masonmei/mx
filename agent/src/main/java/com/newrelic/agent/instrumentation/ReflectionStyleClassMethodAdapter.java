package com.newrelic.agent.instrumentation;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

import com.newrelic.agent.Agent;

public class ReflectionStyleClassMethodAdapter extends AbstractTracingMethodAdapter {
    private final int tracerFactoryId;

    public ReflectionStyleClassMethodAdapter(GenericClassAdapter genericClassAdapter, MethodVisitor mv, int access,
                                             Method method, int tracerFactoryId) {
        super(genericClassAdapter, mv, access, method);
        this.tracerFactoryId = tracerFactoryId;
        if (Agent.LOG.isFinestEnabled()) {
            Agent.LOG.finest("Using fallback instrumentation on " + genericClassAdapter.className + "/" + methodName
                                     + methodDesc);
        }
    }

    protected void loadGetTracerArguments() {
        methodBuilder.loadInvocationHandlerFromProxy();
        methodBuilder.loadInvocationHandlerProxyAndMethod(Integer.valueOf(tracerFactoryId));
        methodBuilder.loadArray(Object.class, new Object[] {genericClassAdapter.className, methodName, methodDesc,
                                                                   MethodBuilder.LOAD_THIS,
                                                                   MethodBuilder.LOAD_ARG_ARRAY});
    }
}