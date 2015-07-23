package com.newrelic.agent.instrumentation;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.SerialVersionUIDAdder;

import com.newrelic.agent.Agent;
import com.newrelic.agent.service.ServiceFactory;

public class InstrumentationUtils {
    public static final int JAVA_5_VERSION_NO = 49;
    public static final int JAVA_6_VERSION_NO = 50;
    public static final int JAVA_7_VERSION_NO = 51;
    private static final int JAVA_CLASS_VERSION_BYTE_OFFSET = 6;

    public static boolean isAbleToResolveAgent(ClassLoader loader, String className) {
        try {
            ClassLoaderCheck.loadAgentClass(loader);
            return true;
        } catch (Throwable t) {
            String msg = MessageFormat.format("Classloader {0} failed to load Agent class. The agent might need to be "
                                                      + "loaded by the bootstrap classloader.: {1}",
                                                     new Object[] {loader.getClass().getName(), t});

            Agent.LOG.finer(msg);
        }
        return false;
    }

    public static ClassWriter getClassWriter(ClassReader cr, ClassLoader classLoader) {
        int writerFlags = 1;
        if (shouldComputeFrames(cr)) {
            writerFlags = 2;
        }
        return new AgentClassWriter(cr, writerFlags, classLoader);
    }

    private static boolean shouldComputeFrames(ClassReader cr) {
        if (getClassJavaVersion(cr) < 50) {
            return false;
        }

        return ServiceFactory.getConfigService().getDefaultAgentConfig().getClassTransformerConfig().computeFrames();
    }

    public static byte[] generateClassBytesWithSerialVersionUID(ClassReader classReader, int classReaderFlags,
                                                                ClassLoader classLoader) {
        ClassWriter cw = getClassWriter(classReader, classLoader);
        ClassVisitor cv = new SerialVersionUIDAdder(cw);
        classReader.accept(cv, classReaderFlags);
        return cw.toByteArray();
    }

    public static byte[] generateClassBytesWithSerialVersionUID(byte[] classBytes, int classReaderFlags,
                                                                ClassLoader classLoader) {
        ClassReader cr = new ClassReader(classBytes);
        return generateClassBytesWithSerialVersionUID(cr, classReaderFlags, classLoader);
    }

    public static boolean isInterface(ClassReader cr) {
        return (cr.getAccess() & 0x200) != 0;
    }

    private static int getClassJavaVersion(ClassReader cr) {
        return cr.readUnsignedShort(6);
    }

    public static Set<org.objectweb.asm.commons.Method> getDeclaredMethods(Class<?> clazz) {
        java.lang.reflect.Method[] methods = clazz.getDeclaredMethods();
        Set result = new HashSet(methods.length);
        for (java.lang.reflect.Method method : methods) {
            result.add(getMethod(method));
        }
        return result;
    }

    public static org.objectweb.asm.commons.Method getMethod(java.lang.reflect.Method method) {
        Class[] params = method.getParameterTypes();
        Type[] args = new Type[params.length];
        for (int i = 0; i < params.length; i++) {
            args[i] = Type.getType(params[i]);
        }
        return new org.objectweb.asm.commons.Method(method.getName(), Type.getType(method.getReturnType()), args);
    }
}