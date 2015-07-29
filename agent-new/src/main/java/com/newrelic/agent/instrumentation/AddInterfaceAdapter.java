package com.newrelic.agent.instrumentation;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.newrelic.agent.Agent;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;

public class AddInterfaceAdapter extends ClassVisitor {
    private final String className;
    private final Class<?> type;

    public AddInterfaceAdapter(ClassVisitor cv, String className, Class<?> type) {
        super(327680, cv);
        this.className = className;
        this.type = type;
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, addInterface(interfaces));
    }

    public void visitEnd() {
        if (Agent.LOG.isFinerEnabled()) {
            String msg = MessageFormat.format("Appended {0} to {1}", new Object[] {this.type.getName(), this.className
                                                                                                                .replace('/',
                                                                                                                                '.')});
            Agent.LOG.finer(msg);
        }
        super.visitEnd();
    }

    private String[] addInterface(String[] interfaces) {
        Set list = new HashSet(Arrays.asList(interfaces));
        list.add(Type.getType(this.type).getInternalName());
        return (String[]) list.toArray(new String[list.size()]);
    }
}