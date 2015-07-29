package com.newrelic.agent.util.asm;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Set;

import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.Method;
import com.newrelic.deps.org.objectweb.asm.tree.MethodNode;
import com.newrelic.deps.org.objectweb.asm.util.Printer;
import com.newrelic.deps.org.objectweb.asm.util.Textifier;
import com.newrelic.deps.org.objectweb.asm.util.TraceClassVisitor;
import com.newrelic.deps.org.objectweb.asm.util.TraceMethodVisitor;

import com.newrelic.deps.com.google.common.base.Joiner;
import com.newrelic.deps.com.google.common.collect.ImmutableSet;
import com.newrelic.agent.Agent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.BootstrapLoader;

public class Utils {
    private static final String PROXY_CLASS_NAME = "java/lang/reflect/Proxy";
    private static final Set<String> JAXB_SUPERCLASSES = ImmutableSet.of("com.sun.xml.internal.bind.v2.runtime.reflect"
                                                                                 + ".Accessor",
                                                                                "com.sun.xml.bind.v2.runtime.reflect"
                                                                                        + ".Accessor",
                                                                                "com.sun.xml.internal.bind.v2.runtime"
                                                                                        + ".unmarshaller.Receiver");

    private static final Set<String> RMI_SUPERCLASSES = ImmutableSet.of("org.omg.stub.javax.management.remote.rmi"
                                                                                + "._RMIConnection_Stub",
                                                                               "com.sun.jmx.remote.internal.ProxyRef");

    private static final Set<String> PRIMITIVE_TYPES = ImmutableSet.of(Type.BOOLEAN_TYPE.getClassName(),
                                                                              Type.BYTE_TYPE.getClassName(),
                                                                              Type.CHAR_TYPE.getClassName(),
                                                                              Type.DOUBLE_TYPE.getClassName(),
                                                                              Type.FLOAT_TYPE.getClassName(),
                                                                              Type.INT_TYPE.getClassName(),
                                                                              new String[] {Type.LONG_TYPE
                                                                                                    .getClassName(),
                                                                                                   Type.SHORT_TYPE
                                                                                                           .getClassName(),
                                                                                                   Type.VOID_TYPE
                                                                                                           .getClassName()});

    public static boolean isJdkProxy(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        return isJdkProxy(reader);
    }

    public static boolean isJdkProxy(ClassReader reader) {
        if ((reader != null) && (looksLikeAProxy(reader))) {
            ProxyClassVisitor cv = new ProxyClassVisitor();
            reader.accept(cv, 1);
            return cv.isProxy();
        }
        return false;
    }

    private static boolean looksLikeAProxy(ClassReader reader) {
        return ("java/lang/reflect/Proxy".equals(reader.getSuperName())) && (Modifier.isFinal(reader.getAccess()));
    }

    public static ClassReader readClass(Class<?> theClass) throws IOException, BenignClassReadException {
        if (theClass.isArray()) {
            throw new BenignClassReadException(theClass.getName() + " is an array");
        }
        if (Proxy.isProxyClass(theClass)) {
            throw new BenignClassReadException(theClass.getName() + " is a Proxy class");
        }
        if (isRMIStubOrProxy(theClass)) {
            throw new BenignClassReadException(theClass.getName() + " is an RMI Stub or Proxy class");
        }
        if (theClass.getName().startsWith("sun.reflect.")) {
            throw new BenignClassReadException(theClass.getName() + " is a reflection class");
        }
        if (isJAXBClass(theClass)) {
            throw new BenignClassReadException(theClass.getName() + " is a JAXB accessor class");
        }
        if ((theClass.getProtectionDomain().getCodeSource() != null) && (theClass.getProtectionDomain().getCodeSource()
                                                                                 .getLocation() == null)) {
            throw new BenignClassReadException(theClass.getName() + " is a generated class");
        }
        URL resource = getClassResource(theClass.getClassLoader(), Type.getInternalName(theClass));
        if (resource == null) {
            ClassReader reader = ServiceFactory.getClassTransformerService().getContextManager().getClassWeaverService()
                                         .getClassReader(theClass);

            if (reader != null) {
                return reader;
            }
        }
        return getClassReaderFromResource(theClass.getName(), resource);
    }

    private static boolean isJAXBClass(Class<?> theClass) {
        if (theClass.getSuperclass() == null) {
            return false;
        }
        return JAXB_SUPERCLASSES.contains(theClass.getSuperclass().getName());
    }

    private static boolean isRMIStubOrProxy(Class<?> theClass) {
        if (theClass.getSuperclass() == null) {
            return false;
        }
        return RMI_SUPERCLASSES.contains(theClass.getSuperclass().getName());
    }

    public static ClassReader readClass(ClassLoader loader, String internalClassName) throws IOException {
        URL resource = getClassResource(loader, internalClassName);
        return getClassReaderFromResource(internalClassName, resource);
    }

    public static ClassReader getClassReaderFromResource(String internalClassName, URL resource) throws IOException {
        if (resource != null) {
            InputStream stream = resource.openStream();
            try {
                return new ClassReader(stream);
            } finally {
                stream.close();
            }
        }
        throw new MissingResourceException("Unable to get the resource stream for class " + internalClassName);
    }

    public static String getClassResourceName(String internalName) {
        return internalName + ".class";
    }

    public static String getClassResourceName(Class<?> clazz) {
        return getClassResourceName(Type.getInternalName(clazz));
    }

    public static URL getClassResource(ClassLoader loader, Type type) {
        return getClassResource(loader, type.getInternalName());
    }

    public static URL getClassResource(ClassLoader loader, String internalClassName) {
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        if ((Agent.LOG.isFinestEnabled()) && (internalClassName.endsWith(".class.class"))) {
            Agent.LOG.finest("Invalid resource name " + internalClassName);
        }
        URL url = loader.getResource(getClassResourceName(internalClassName));
        if (url == null) {
            url = BootstrapLoader.get().getBootstrapResource(internalClassName);
        }
        return url;
    }

    public static void print(byte[] bytes) {
        print(bytes, new PrintWriter(System.out, true));
    }

    public static String asString(MethodNode method) {
        Printer printer = new Textifier();
        TraceMethodVisitor tv = new TraceMethodVisitor(printer);

        method.accept(tv);

        return Joiner.on(' ').join(printer.getText());
    }

    public static void print(byte[] bytes, PrintWriter pw) {
        ClassReader cr = new ClassReader(bytes);
        TraceClassVisitor mv = new TraceClassVisitor(pw);
        cr.accept(mv, 8);
        pw.flush();
    }

    public static boolean isPrimitiveType(String type) {
        return PRIMITIVE_TYPES.contains(type);
    }

    public static int getFirstLocal(int access, Method method) {
        Type[] argumentTypes = method.getArgumentTypes();
        int nextLocal = (0x8 & access) == 0 ? 1 : 0;
        for (int i = 0; i < argumentTypes.length; i++) {
            nextLocal += argumentTypes[i].getSize();
        }
        return nextLocal;
    }
}