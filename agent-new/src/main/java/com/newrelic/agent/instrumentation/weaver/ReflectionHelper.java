package com.newrelic.agent.instrumentation.weaver;

import java.util.Map;

import com.newrelic.agent.bridge.reflect.ClassReflection;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.GeneratorAdapter;

class ReflectionHelper {
    private static final ReflectionHelper INSTANCE = new ReflectionHelper();
    private final Map<String, ClassReflector> classes;

    public ReflectionHelper() {
        this.classes = Maps.newHashMap();

        for (java.lang.reflect.Method m : ClassReflection.class.getMethods()) {
            com.newrelic.deps.org.objectweb.asm.commons.Method staticMethod =
                    com.newrelic.deps.org.objectweb.asm.commons.Method.getMethod(m);

            if ((m.getDeclaringClass().equals(ClassReflection.class)) && (staticMethod.getArgumentTypes().length > 0)) {
                Type targetClass = staticMethod.getArgumentTypes()[0];
                Class[] args = new Class[m.getParameterTypes().length - 1];
                System.arraycopy(m.getParameterTypes(), 1, args, 0, staticMethod.getArgumentTypes().length - 1);
                try {
                    m.getParameterTypes()[0].getMethod(m.getName(), args);

                    ClassReflector classReflector = (ClassReflector) this.classes.get(targetClass.getInternalName());
                    if (classReflector == null) {
                        classReflector = new ClassReflector();
                        this.classes.put(targetClass.getInternalName(), classReflector);
                    }

                    Type[] argumentTypes = new Type[staticMethod.getArgumentTypes().length - 1];
                    System.arraycopy(staticMethod.getArgumentTypes(), 1, argumentTypes, 0,
                                            staticMethod.getArgumentTypes().length - 1);

                    com.newrelic.deps.org.objectweb.asm.commons.Method targetMethod =
                            new com.newrelic.deps.org.objectweb.asm.commons.Method(m.getName(),
                                                                                          staticMethod.getReturnType(),
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
        ClassReflector classReflector = (ClassReflector) this.classes.get(owner);
        if (classReflector != null) {
            com.newrelic.deps.org.objectweb.asm.commons.Method method =
                    (com.newrelic.deps.org.objectweb.asm.commons.Method) classReflector.methods
                                                                                 .get(new com.newrelic.deps.org.objectweb.asm.commons.Method(name,
                                                                                                                                                    desc));
            if (method != null) {
                generatorAdapter.invokeStatic(Type.getType(ClassReflection.class), method);
                return true;
            }
        }
        return false;
    }

    private static class ClassReflector {
        private final Map<com.newrelic.deps.org.objectweb.asm.commons.Method, com.newrelic.deps.org.objectweb.asm.commons.Method>
                methods = Maps.newHashMap();
    }
}