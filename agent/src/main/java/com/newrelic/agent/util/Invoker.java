package com.newrelic.agent.util;

import java.lang.reflect.Method;

import org.objectweb.asm.Type;

public class Invoker {
    public static Object invoke(Object called, Class<?> clazz, String methodName, Object[] args) throws Exception {
        Class[] argTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = args[i].getClass();
        }
        return invoke(called, clazz, methodName, argTypes, args);
    }

    private static Object invoke(Object called, Class<?> clazz, String methodName, Class<?>[] argTypes, Object[] args)
            throws Exception {
        Method method = clazz.getMethod(methodName, argTypes);
        method.setAccessible(true);
        return method.invoke(called, args);
    }

    public static String getClassNameFromInternalName(String className) {
        return Type.getObjectType(className).getClassName();
    }
}