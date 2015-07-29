package com.newrelic.agent.instrumentation;

import java.io.IOException;
import java.io.InputStream;

import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.Type;

class ClassMetadata {
    private static final int HEADER_MODIFIERS_OFFSET = 0;
    private static final int HEADER_SUPER_CLASS_CONSTANT_POOL_OFFSET = 4;
    private static final int HEADER_NUM_INTERFACES_OFFSET = 6;
    private static final int HEADER_INITIAL_INTERFACE_OFFSET = 8;
    int modifiers;
    String superClass;
    ClassMetadata superClassMetadata;
    String[] interfaces;
    ClassMetadata[] interfaceMetadata;
    private Type type;
    private ClassLoader classLoader;
    private ClassReader cr;

    public ClassMetadata(String type, ClassLoader loader) {
        classLoader = (loader == null ? ClassLoader.getSystemClassLoader() : loader);
        this.type = Type.getObjectType(type);
        cr = getClassReader(type, classLoader);
        modifiers = cr.readUnsignedShort(cr.header + HEADER_MODIFIERS_OFFSET);

        char[] buf = new char[2048];
        int cpSuperIndex = cr.getItem(cr.readUnsignedShort(cr.header + HEADER_SUPER_CLASS_CONSTANT_POOL_OFFSET));
        superClass = (cpSuperIndex == 0 ? null : cr.readUTF8(cpSuperIndex, buf));
        interfaces = new String[cr.readUnsignedShort(cr.header + HEADER_NUM_INTERFACES_OFFSET)];
        int nextInterface = cr.header + HEADER_INITIAL_INTERFACE_OFFSET;
        for (int i = 0; i < interfaces.length; i++) {
            interfaces[i] = cr.readClass(nextInterface, buf);
            nextInterface += 2;
        }
    }

    public ClassReader getClassReader() {
        return cr;
    }

    String[] getInterfaceNames() {
        return interfaces;
    }

    private ClassReader getClassReader(String type, ClassLoader classLoader) {
        String resource = type.replace('.', '/') + ".class";
        InputStream is = null;
        ClassReader cr;
        try {
            is = classLoader.getResourceAsStream(resource);
            cr = new ClassReader(is);
        } catch (IOException e) {
            throw new RuntimeException("unable to access resource: " + resource, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
        }
        return cr;
    }

    String getName() {
        return type.getInternalName();
    }

    ClassMetadata getSuperclass() {
        if (superClass == null) {
            return null;
        }
        if (superClassMetadata == null) {
            superClassMetadata = new ClassMetadata(superClass, classLoader);
        }
        return superClassMetadata;
    }

    ClassMetadata[] getInterfaces() {
        if (interfaceMetadata == null) {
            interfaceMetadata = new ClassMetadata[interfaces.length];
            for (int i = 0; i < interfaces.length; i++) {
                interfaceMetadata[i] = new ClassMetadata(interfaces[i], classLoader);
            }
        }
        return interfaceMetadata;
    }

    boolean isInterface() {
        return (modifiers & 0x200) > 0;
    }

    private boolean implementsInterface(ClassMetadata other) {
        if (this == other) {
            return true;
        }

        for (ClassMetadata c = this; c != null; c = c.getSuperclass()) {
            for (ClassMetadata in : c.getInterfaces()) {
                if ((in.type.equals(other.type)) || (in.implementsInterface(other))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSubclassOf(ClassMetadata other) {
        for (ClassMetadata c = this; c != null; c = c.getSuperclass()) {
            ClassMetadata sc = c.getSuperclass();
            if ((sc != null) && (sc.type.equals(other.type))) {
                return true;
            }
        }
        return false;
    }

    public boolean isAssignableFrom(ClassMetadata other) {
        return (this == other) || (other.implementsInterface(this)) || (other.isSubclassOf(this))
                       || ((other.isInterface()) && (type.getDescriptor().equals("Ljava/lang/Object;")));
    }
}