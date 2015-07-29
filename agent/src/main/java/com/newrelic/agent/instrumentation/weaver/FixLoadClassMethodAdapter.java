package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.agent.Agent;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.GeneratorAdapter;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

final class FixLoadClassMethodAdapter extends GeneratorAdapter {
    FixLoadClassMethodAdapter(int access, Method method, MethodVisitor mv) {
        super(Agent.ASM_LEVEL, mv, access, method.getName(), method.getDescriptor());
    }

    public void visitLdcInsn(Object cst) {
        if ((cst instanceof Type)) {
            Type type = (Type) cst;

            super.visitLdcInsn(type.getClassName());

            invokeStatic(Type.getType(Class.class), new Method("forName", Type.getType(Class.class),
                                                                      new Type[] {Type.getType(String.class)}));
        } else {
            super.visitLdcInsn(cst);
        }
    }
}