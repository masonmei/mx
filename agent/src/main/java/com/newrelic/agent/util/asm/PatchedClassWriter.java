package com.newrelic.agent.util.asm;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.logging.Level;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import com.google.common.collect.Sets;
import com.newrelic.agent.Agent;

public class PatchedClassWriter extends ClassWriter {
    private static final String JAVA_LANG_OBJECT = "java/lang/Object";
    protected final ClassResolver classResolver;

    public PatchedClassWriter(int flags, ClassLoader classLoader) {
        this(flags, ClassResolvers.getClassLoaderResolver(classLoader == null ? ClassLoader.getSystemClassLoader()
                                                                  : classLoader));
    }

    public PatchedClassWriter(int flags, ClassResolver classResolver) {
        super(flags);

        this.classResolver = classResolver;
    }

    protected String getCommonSuperClass(String type1, String type2) {
        if (type1.equals(type2)) {
            return type1;
        }
        if (("java/lang/Object".equals(type1)) || ("java/lang/Object".equals(type2))) {
            return "java/lang/Object";
        }
        try {
            ClassReader reader1 = getClassReader(type1);
            ClassReader reader2 = getClassReader(type2);

            if ((reader1 == null) || (reader2 == null)) {
                return "java/lang/Object";
            }

            String superClass = getCommonSuperClass(reader1, reader2);

            if (superClass == null) {
                return "java/lang/Object";
            }
            return superClass;
        } catch (Exception ex) {
            Agent.LOG.log(Level.FINER, ex, "Unable to get common super class", new Object[0]);
            throw new RuntimeException(ex.toString());
        }
    }

    protected ClassReader getClassReader(String type) throws IOException {
        InputStream classResource = null;
        try {
            classResource = classResolver.getClassResource(type);
            return classResource == null ? null : new ClassReader(classResource);
        } catch (IOException ex) {
            Agent.LOG.log(Level.FINEST, ex.toString(), ex);
            return null;
        } finally {
            if (classResource != null) {
                classResource.close();
            }
        }
    }

    private String getCommonSuperClass(ClassReader reader1, ClassReader reader2)
            throws ClassNotFoundException, IOException {
        if (isAssignableFrom(reader1, reader2)) {
            return reader1.getClassName();
        }
        if (isAssignableFrom(reader2, reader1)) {
            return reader2.getClassName();
        }

        if ((isInterface(reader1)) || (isInterface(reader2))) {
            return "java/lang/Object";
        }

        Set classes = Sets.newHashSet();
        classes.add(reader1.getClassName());
        while (reader1.getSuperName() != null) {
            classes.add(reader1.getSuperName());
            reader1 = getClassReader(reader1.getSuperName());
        }

        while (reader2.getSuperName() != null) {
            if (classes.contains(reader2.getClassName())) {
                return reader2.getClassName();
            }
            reader2 = getClassReader(reader2.getSuperName());
        }

        return null;
    }

    private boolean isInterface(ClassReader reader) {
        return (reader.getAccess() & 0x200) != 0;
    }

    private boolean isAssignableFrom(ClassReader reader1, ClassReader reader2) {
        return (reader1.getClassName().equals(reader2.getClassName())) || (reader1.getClassName()
                                                                                   .equals(reader2.getSuperName()));
    }
}