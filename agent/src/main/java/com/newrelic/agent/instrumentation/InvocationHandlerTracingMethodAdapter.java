package com.newrelic.agent.instrumentation;

import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

public class InvocationHandlerTracingMethodAdapter extends AbstractTracingMethodAdapter {
    public InvocationHandlerTracingMethodAdapter(GenericClassAdapter genericClassAdapter, MethodVisitor mv, int access,
                                                 Method method) {
        super(genericClassAdapter, mv, access, method);
    }

    protected void loadGetTracerArguments() {
        getStatic(Type.getObjectType(genericClassAdapter.className), "__nr__InvocationHandlers",
                         MethodBuilder.INVOCATION_HANDLER_ARRAY_TYPE);

        push(getInvocationHandlerIndex());
        arrayLoad(getTracerType());

        methodBuilder.loadInvocationHandlerProxyAndMethod(null)
                .loadArray(Object.class, new Object[] {MethodBuilder.LOAD_THIS, MethodBuilder.LOAD_ARG_ARRAY});
    }
}