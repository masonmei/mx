//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.util;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import com.google.common.collect.Sets;
import com.newrelic.agent.Agent;

public class ClassUtils {
    public ClassUtils() {
    }

    public static Method findSuperDefinition(Method method) {
        return findSuperDefinition(method.getDeclaringClass(), method);
    }

    private static Method findSuperDefinition(Class<?> clazz, Method method) {
        Class[] interfaces = clazz.getInterfaces();
        Class[] parentClass = interfaces;
        int e = interfaces.length;
        int i$ = 0;

        while (i$ < e) {
            Class interfaceClass = parentClass[i$];

            try {
                return interfaceClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
            } catch (Exception var9) {
                method = findSuperDefinition(interfaceClass, method);
                ++i$;
            }
        }

        Class var10 = clazz.getSuperclass();
        if (var10 != null) {
            try {
                method = var10.getDeclaredMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException var8) {
                ;
            }

            return findSuperDefinition(var10, method);
        } else {
            return method;
        }
    }

    public static Set<String> getClassReferences(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        final HashSet classNames = Sets.newHashSet();
        ClassVisitor cv = new ClassVisitor(Agent.ASM_LEVEL) {
            public void visit(int version, int access, String name, String signature, String superName,
                              String[] interfaces) {
                this.addType(Type.getObjectType(superName));
                String[] arr$ = interfaces;
                int len$ = interfaces.length;

                for (int i$ = 0; i$ < len$; ++i$) {
                    String iFace = arr$[i$];
                    this.addType(Type.getObjectType(iFace));
                }

            }

            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                this.addType(Type.getType(desc));
                return null;
            }

            private void addType(Type type) {
                if (type != null) {
                    if (type.getSort() == 9) {
                        this.addType(type.getElementType());
                    } else if (type.getSort() == 10) {
                        classNames.add(type.getInternalName());
                    }

                }
            }

            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                             String[] exceptions) {
                this.addMethodClasses(name, desc);
                return new MethodVisitor(Agent.ASM_LEVEL) {
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
                org.objectweb.asm.commons.Method method = new org.objectweb.asm.commons.Method(name, desc);
                Type[] arr$ = method.getArgumentTypes();
                int len$ = arr$.length;

                for (int i$ = 0; i$ < len$; ++i$) {
                    Type t = arr$[i$];
                    this.addType(t);
                }

            }
        };
        cr.accept(cv, 4);
        return classNames;
    }
}
