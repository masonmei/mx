package com.newrelic.agent.instrumentation;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.pointcuts.MethodMapper;

public class MethodMappersAdapter extends ClassVisitor {
    private final Map<org.objectweb.asm.commons.Method, java.lang.reflect.Method> methods;
    private final String className;
    private final String originalInterface;

    private MethodMappersAdapter(ClassVisitor cv,
                                 Map<org.objectweb.asm.commons.Method, java.lang.reflect.Method> methods,
                                 String originalInterface, String className) {
        super(Agent.ASM_LEVEL, cv);
        this.methods = methods;
        this.originalInterface = originalInterface;
        this.className = className;
    }

    protected static Map<org.objectweb.asm.commons.Method, java.lang.reflect.Method> getMethodMappers(Class<?> type) {
        Map methods = new HashMap();
        for (java.lang.reflect.Method method : type.getDeclaredMethods()) {
            MethodMapper annotation = (MethodMapper) method.getAnnotation(MethodMapper.class);
            if (annotation == null) {
                throw new RuntimeException("Method " + method.getName() + " does not have a MethodMapper annotation");
            }
            String originalMethodName = annotation.originalMethodName();
            String orginalDescriptor = annotation.originalDescriptor();
            if ("".equals(orginalDescriptor)) {
                orginalDescriptor = InstrumentationUtils.getMethod(method).getDescriptor();
            }
            if (method.getName().equals(annotation.originalMethodName())) {
                String msg = MessageFormat
                                     .format("Ignoring {0} method in {1}: method name is same as orginalMethodName",
                                                    new Object[] {method.getName(), type.getClass().getName()});

                Agent.LOG.fine(msg);
            } else {
                methods.put(new org.objectweb.asm.commons.Method(originalMethodName, orginalDescriptor), method);
            }
        }
        return methods;
    }

    public static MethodMappersAdapter getMethodMappersAdapter(ClassVisitor cv,
                                                               Map<org.objectweb.asm.commons.Method, java.lang
                                                                                                             .reflect
                                                                                                             .Method>
                                                                       methods,
                                                               String originalInterface, String className) {
        Map methods2 = new HashMap(methods);
        return new MethodMappersAdapter(cv, methods2, originalInterface, className);
    }

    public static MethodMappersAdapter getMethodMappersAdapter(ClassVisitor cv, Class<?> type, String className) {
        Map methods = getMethodMappers(type);
        return new MethodMappersAdapter(cv, methods, type.getName(), className);
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        org.objectweb.asm.commons.Method originalMethod = new org.objectweb.asm.commons.Method(name, desc);
        java.lang.reflect.Method method = (java.lang.reflect.Method) methods.remove(originalMethod);
        if (method != null) {
            addMethod(method, originalMethod);
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    private void addMethod(java.lang.reflect.Method method, org.objectweb.asm.commons.Method originalMethod) {
        org.objectweb.asm.commons.Method newMethod = InstrumentationUtils.getMethod(method);

        MethodMapper methodMapper = (MethodMapper) method.getAnnotation(MethodMapper.class);

        Type returnType = Type.getType(method.getReturnType());
        GeneratorAdapter mv = new GeneratorAdapter(1, newMethod, null, null, this);
        mv.visitCode();

        mv.loadThis();

        for (int i = 0; i < newMethod.getArgumentTypes().length; i++) {
            mv.loadArg(i);
        }

        if (methodMapper.invokeInterface()) {
            mv.invokeInterface(Type.getObjectType(originalInterface), originalMethod);
        } else {
            mv.invokeVirtual(Type.getObjectType(className), originalMethod);
        }
        mv.visitInsn(returnType.getOpcode(172));
        mv.visitMaxs(0, 0);
    }
}