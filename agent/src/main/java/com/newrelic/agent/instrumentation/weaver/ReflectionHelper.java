package com.newrelic.agent.instrumentation.weaver;

import java.util.Map;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import com.google.common.collect.Maps;
import com.newrelic.agent.bridge.reflect.ClassReflection;

class ReflectionHelper {
    private static final ReflectionHelper INSTANCE = new ReflectionHelper();
    private final Map<String, ClassReflector> classes;

    public ReflectionHelper() {
        classes = Maps.newHashMap();

        for (java.lang.reflect.Method m : ClassReflection.class.getMethods()) {
            org.objectweb.asm.commons.Method staticMethod = org.objectweb.asm.commons.Method.getMethod(m);

            if ((m.getDeclaringClass().equals(ClassReflection.class)) && (staticMethod.getArgumentTypes().length > 0)) {
                Type targetClass = staticMethod.getArgumentTypes()[0];
                Class[] args = new Class[m.getParameterTypes().length - 1];
                System.arraycopy(m.getParameterTypes(), 1, args, 0, staticMethod.getArgumentTypes().length - 1);
                try {
                    m.getParameterTypes()[0].getMethod(m.getName(), args);

                    ClassReflector classReflector = (ClassReflector) classes.get(targetClass.getInternalName());
                    if (classReflector == null) {
                        classReflector = new ClassReflector();
                        classes.put(targetClass.getInternalName(), classReflector);
                    }

                    Type[] argumentTypes = new Type[staticMethod.getArgumentTypes().length - 1];
                    System.arraycopy(staticMethod.getArgumentTypes(), 1, argumentTypes, 0,
                                            staticMethod.getArgumentTypes().length - 1);

                    org.objectweb.asm.commons.Method targetMethod =
                            new org.objectweb.asm.commons.Method(m.getName(), staticMethod.getReturnType(),
                                                                        argumentTypes);
                    classReflector.methods.put(targetMethod, staticMethod);
                } catch (NoSuchMethodException ex) {
                }
            }
        }
    }

    public static ReflectionHelper get() {
        return INSTANCE;
    }

    public boolean process(String owner, String name, String desc, GeneratorAdapter generatorAdapter) {
        ClassReflector classReflector = (ClassReflector) classes.get(owner);
        if (classReflector != null) {
            org.objectweb.asm.commons.Method method = (org.objectweb.asm.commons.Method) classReflector.methods
                                                                                                 .get(new org.objectweb.asm.commons.Method(name,
                                                                                                                                                  desc));
            if (method != null) {
                generatorAdapter.invokeStatic(Type.getType(ClassReflection.class), method);
                return true;
            }
        }
        return false;
    }

    private static class ClassReflector {
        private final Map<org.objectweb.asm.commons.Method, org.objectweb.asm.commons.Method> methods =
                Maps.newHashMap();
    }
}