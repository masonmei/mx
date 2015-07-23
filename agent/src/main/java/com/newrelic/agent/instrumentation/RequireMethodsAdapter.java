//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;

import com.newrelic.agent.Agent;

public class RequireMethodsAdapter extends ClassVisitor {
    private final Set<Method> requiredMethods;
    private final ClassLoader classLoader;
    private final String className;
    private final String requiredInterface;
    private final ClassVisitor missingMethodsVisitor = new RequireMethodsAdapter.MissingMethodsVisitor();

    private RequireMethodsAdapter(ClassVisitor cv, Set<Method> requiredMethods, String requiredInterface,
                                  String className, ClassLoader loader) {
        super(Agent.ASM_LEVEL, cv);
        this.className = className;
        this.requiredInterface = requiredInterface;
        this.classLoader = loader;
        this.requiredMethods = new HashSet(requiredMethods);
    }

    public static RequireMethodsAdapter getRequireMethodsAdaptor(ClassVisitor cv, String className, Class<?> type,
                                                                 ClassLoader loader) {
        Set requiredMethods = InstrumentationUtils.getDeclaredMethods(type);
        return new RequireMethodsAdapter(cv, requiredMethods, type.getName(), className, loader);
    }

    public static RequireMethodsAdapter getRequireMethodsAdaptor(ClassVisitor cv, Set<Method> requiredMethods,
                                                                 String className, String requiredInterface,
                                                                 ClassLoader loader) {
        return new RequireMethodsAdapter(cv, requiredMethods, requiredInterface, className, loader);
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        this.requiredMethods.remove(new Method(name, desc));
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    public void visitEnd() {
        if (this.requiredMethods.size() > 0) {
            this.visitSuperclassesOrInterfaces();
        }

        if (this.requiredMethods.size() > 0) {
            String msg = MessageFormat.format("{0} does not implement these methods: {1} declared in {2}",
                                                     new Object[] {this.className, this.requiredMethods,
                                                                          this.requiredInterface});
            throw new StopProcessingException(msg);
        } else {
            super.visitEnd();
        }
    }

    private void visitSuperclassesOrInterfaces() {
        ClassMetadata metadata = new ClassMetadata(this.className, this.classLoader);
        if (metadata.isInterface()) {
            this.visitInterfaces(metadata);
        } else {
            this.visitSuperclasses(metadata);
        }

    }

    private void visitSuperclasses(ClassMetadata metadata) {
        for (ClassMetadata superClassMetadata = metadata.getSuperclass(); superClassMetadata != null;
             superClassMetadata = superClassMetadata.getSuperclass()) {
            ClassReader cr = superClassMetadata.getClassReader();
            cr.accept(this.missingMethodsVisitor, 0);
            if (this.requiredMethods.size() == 0) {
                return;
            }
        }

    }

    private void visitInterfaces(ClassMetadata metadata) {
        LinkedList pendingInterfaces = new LinkedList();
        pendingInterfaces.addAll(Arrays.asList(metadata.getInterfaceNames()));

        for (String interfaceName = (String) pendingInterfaces.poll(); interfaceName != null;
             interfaceName = (String) pendingInterfaces.poll()) {
            ClassMetadata interfaceMetadata = new ClassMetadata(interfaceName, this.classLoader);
            ClassReader cr = interfaceMetadata.getClassReader();
            cr.accept(this.missingMethodsVisitor, 0);
            if (this.requiredMethods.size() == 0) {
                return;
            }

            pendingInterfaces.addAll(Arrays.asList(interfaceMetadata.getInterfaceNames()));
        }

    }

    private class MissingMethodsVisitor extends ClassVisitor {
        private MissingMethodsVisitor() {
            super(Agent.ASM_LEVEL);
        }

        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            RequireMethodsAdapter.this.requiredMethods.remove(new Method(name, desc));
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
    }
}
