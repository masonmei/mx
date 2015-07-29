package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.deps.org.objectweb.asm.AnnotationVisitor;
import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;

import com.newrelic.agent.Agent;
import com.newrelic.api.agent.weaver.internal.NewClass;

public class NewClassMarker {
    private static final String NEW_CLASS_INTERNAL_NAME = Type.getInternalName(NewClass.class);

    static ClassVisitor getVisitor(ClassVisitor cv, final String implementationTitle,
                                   final String implementationVersion) {
        return new ClassVisitor(Agent.ASM_LEVEL, cv) {
            public void visit(int version, int access, String name, String signature, String superName,
                              String[] interfaces) {
                String[] newInterfaces = new String[interfaces.length + 1];
                System.arraycopy(interfaces, 0, newInterfaces, 0, interfaces.length);
                newInterfaces[interfaces.length] = Type.getInternalName(NewClass.class);

                super.visit(version, access, name, signature, superName, newInterfaces);

                AnnotationVisitor visitor = super.visitAnnotation(Type.getDescriptor(WeaveInstrumentation.class), true);
                visitor.visit("title", implementationTitle);
                visitor.visit("version", implementationVersion);
                visitor.visitEnd();
            }
        };
    }

    public static boolean isNewWeaveClass(ClassReader reader) {
        for (String className : reader.getInterfaces()) {
            if (NEW_CLASS_INTERNAL_NAME.equals(className)) {
                return true;
            }
        }
        return false;
    }
}