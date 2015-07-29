package com.newrelic.agent.util.asm;

import java.lang.reflect.Method;

import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.FieldVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;

import com.newrelic.agent.Agent;

public class ProxyClassVisitor extends ClassVisitor {
    private static final String PROXY_METHOD_DESC = Type.getDescriptor(Method.class);

    private boolean hasProxyMethod = false;

    public ProxyClassVisitor() {
        super(Agent.ASM_LEVEL);
    }

    public ProxyClassVisitor(ClassVisitor cv) {
        super(Agent.ASM_LEVEL, cv);
    }

    public boolean isProxy() {
        return hasProxyMethod;
    }

    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if ((!hasProxyMethod) && (desc.equals(PROXY_METHOD_DESC))) {
            hasProxyMethod = true;
        }
        return super.visitField(access, name, desc, signature, value);
    }
}