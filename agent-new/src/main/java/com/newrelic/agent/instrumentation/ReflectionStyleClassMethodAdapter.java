package com.newrelic.agent.instrumentation;

import com.newrelic.agent.Agent;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

public class ReflectionStyleClassMethodAdapter extends AbstractTracingMethodAdapter {
    private final int tracerFactoryId;

    public ReflectionStyleClassMethodAdapter(GenericClassAdapter genericClassAdapter, MethodVisitor mv, int access,
                                             Method method, int tracerFactoryId) {
        super(genericClassAdapter, mv, access, method);
        this.tracerFactoryId = tracerFactoryId;
        if (Agent.LOG.isFinestEnabled()) {
            Agent.LOG
                    .finest("Using fallback instrumentation on " + genericClassAdapter.className + "/" + this.methodName
                                    + this.methodDesc);
        }
    }

    protected void loadGetTracerArguments() {
        this.methodBuilder.loadInvocationHandlerFromProxy();
        this.methodBuilder.loadInvocationHandlerProxyAndMethod(Integer.valueOf(this.tracerFactoryId));
        this.methodBuilder.loadArray(Object.class, new Object[] {this.genericClassAdapter.className, this.methodName,
                                                                        this.methodDesc, MethodBuilder.LOAD_THIS,
                                                                        MethodBuilder.LOAD_ARG_ARRAY});
    }
}