package com.newrelic.agent.util;

import java.util.Set;

import com.newrelic.deps.com.google.common.collect.Sets;
import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.FieldVisitor;
import com.newrelic.deps.org.objectweb.asm.Label;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;

public class ClassUtils {
    public static java.lang.reflect.Method findSuperDefinition(java.lang.reflect.Method method) {
        return findSuperDefinition(method.getDeclaringClass(), method);
    }

    private static java.lang.reflect.Method findSuperDefinition(Class<?> clazz, java.lang.reflect.Method method) {
        Class[] interfaces = clazz.getInterfaces();
        for (Class interfaceClass : interfaces) {
            try {
                return interfaceClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
            } catch (Exception e) {
                method = findSuperDefinition(interfaceClass, method);
            }
        }
        Class parentClass = clazz.getSuperclass();
        if (parentClass != null) {
            try {
                method = parentClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException e) {
            }
            return findSuperDefinition(parentClass, method);
        }
        return method;
    }

    public static Set<String> getClassReferences(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);

        final Set classNames = Sets.newHashSet();
        ClassVisitor cv = new ClassVisitor(327680) {
            public void visit(int version, int access, String name, String signature, String superName,
                              String[] interfaces) {
                addType(Type.getObjectType(superName));
                for (String iFace : interfaces) {
                    addType(Type.getObjectType(iFace));
                }
            }

            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                addType(Type.getType(desc));
                return null;
            }

            private void addType(Type type) {
                if (type == null) {
                    return;
                }
                if (type.getSort() == 9) {
                    addType(type.getElementType());
                } else if (type.getSort() == 10) {
                    classNames.add(type.getInternalName());
                }
            }

            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                             String[] exceptions) {
                addMethodClasses(name, desc);

                return new MethodVisitor(327680) {
                    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                        addType(Type.getType(desc));
                    }

                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        addMethodClasses(name, desc);
                    }

                    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end,
                                                   int index) {
                        addType(Type.getType(desc));
                    }
                };
            }

            private void addMethodClasses(String name, String desc) {
                com.newrelic.deps.org.objectweb.asm.commons.Method method =
                        new com.newrelic.deps.org.objectweb.asm.commons.Method(name, desc);
                for (Type t : method.getArgumentTypes()) {
                    addType(t);
                }
            }
        };
        cr.accept(cv, 4);

        return classNames;
    }
}