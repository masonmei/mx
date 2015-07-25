package com.newrelic.agent.instrumentation;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

import com.newrelic.agent.Agent;

public class AddInterfaceAdapter extends ClassVisitor {
    private final String className;
    private final Class<?> type;

    public AddInterfaceAdapter(ClassVisitor cv, String className, Class<?> type) {
        super(Agent.ASM_LEVEL, cv);
        this.className = className;
        this.type = type;
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, addInterface(interfaces));
    }

    public void visitEnd() {
        if (Agent.LOG.isFinerEnabled()) {
            String msg = MessageFormat.format("Appended {0} to {1}", type.getName(), className.replace('/', '.'));
            Agent.LOG.finer(msg);
        }
        super.visitEnd();
    }

    private String[] addInterface(String[] interfaces) {
        Set<String> list = new HashSet<String>(Arrays.asList(interfaces));
        list.add(Type.getType(type).getInternalName());
        return list.toArray(new String[list.size()]);
    }
}