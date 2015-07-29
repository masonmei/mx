package com.newrelic.agent.util.asm;

import java.lang.reflect.Method;

import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.FieldVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;

public class ProxyClassVisitor extends ClassVisitor {
    private static final String PROXY_METHOD_DESC = Type.getDescriptor(Method.class);

    private boolean hasProxyMethod = false;

    public ProxyClassVisitor() {
        super(327680);
    }

    public ProxyClassVisitor(ClassVisitor cv) {
        super(327680, cv);
    }

    public boolean isProxy() {
        return this.hasProxyMethod;
    }

    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if ((!this.hasProxyMethod) && (desc.equals(PROXY_METHOD_DESC))) {
            this.hasProxyMethod = true;
        }
        return super.visitField(access, name, desc, signature, value);
    }
}