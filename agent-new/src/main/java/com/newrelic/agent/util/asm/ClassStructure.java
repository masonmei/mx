//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.util.asm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.reflect.ClassReflection;
import com.newrelic.deps.com.google.common.collect.ImmutableMap;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.org.objectweb.asm.AnnotationVisitor;
import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.FieldVisitor;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.Method;
import com.newrelic.deps.org.objectweb.asm.tree.FieldNode;

public class ClassStructure {
    public static final int METHODS = 1;
    public static final int FIELDS = 2;
    public static final int CLASS_ANNOTATIONS = 4;
    public static final int METHOD_ANNOTATIONS = 8;
    public static final int ALL = 15;
    private static final ClassStructure.MethodDetails EMPTY_METHOD_DEFAULTS_MEMBER =
            new ClassStructure.MethodDetails(ImmutableMap.<String, AnnotationDetails>of(), false);
    private static final ClassStructure.MethodDetails EMPTY_METHOD_DEFAULTS_STATIC =
            new ClassStructure.MethodDetails(ImmutableMap.<String, AnnotationDetails>of(), true);
    protected final int access;
    protected final String superName;
    protected final String[] interfaces;
    private final Type type;
    protected Map<String, AnnotationDetails> classAnnotations;
    private Map<Method, ClassStructure.MethodDetails> methods;
    private Map<String, FieldNode> fields;

    private ClassStructure(String className, int access, String superName, String[] interfaceNames) {
        this.type = Type.getObjectType(className);
        this.access = access;
        this.superName = superName;
        this.interfaces = interfaceNames;
    }

    public static ClassStructure getClassStructure(URL url) throws IOException {
        return getClassStructure((URL) url, 1);
    }

    public static ClassStructure getClassStructure(URL url, int flags) throws IOException {
        return getClassStructure(Utils.getClassReaderFromResource(url.getPath(), url), flags);
    }

    public static ClassStructure getClassStructure(ClassReader cr, int flags) throws IOException {
        ClassStructure structure =
                new ClassStructure(cr.getClassName(), cr.getAccess(), cr.getSuperName(), cr.getInterfaces());
        ClassVisitor cv = structure.createClassVisitor(flags);
        if (cv != null) {
            cr.accept(cv, 1);
        }

        structure.methods =
                structure.methods == null ? Collections.EMPTY_MAP : Collections.unmodifiableMap(structure.methods);
        structure.classAnnotations = structure.classAnnotations == null ? Collections.EMPTY_MAP
                                             : Collections.unmodifiableMap(structure.classAnnotations);
        structure.fields =
                structure.fields == null ? Collections.EMPTY_MAP : Collections.unmodifiableMap(structure.fields);
        return structure;
    }

    public static ClassStructure getClassStructure(File jarFile, String internalName, int flags)
            throws IOException, ClassNotFoundException {
        JarFile jar = new JarFile(jarFile);

        ClassStructure var6;
        try {
            JarEntry entry = jar.getJarEntry(internalName + ".class");
            if (entry == null) {
                throw new ClassNotFoundException("Unable to find " + internalName + " in " + jarFile.getAbsolutePath());
            }

            InputStream inputStream = jar.getInputStream(entry);

            try {
                var6 = getClassStructure(new ClassReader(inputStream), flags);
            } finally {
                inputStream.close();
            }
        } finally {
            jar.close();
        }

        return var6;
    }

    public static ClassStructure getClassStructure(Class<?> clazz) {
        return getClassStructure((Class) clazz, 1);
    }

    public static ClassStructure getClassStructure(final Class<?> clazz, final int flags) {
        byte access = 0;
        int modifiers = clazz.getModifiers();
        String superName = null;
        int var15;
        if (clazz.isAnnotation()) {
            var15 = access | 8704;
            if (!Modifier.isPrivate(modifiers)) {
                var15 |= 1;
            }

            superName = "java/lang/Object";
        } else if (clazz.isInterface()) {
            var15 = access | 512;
            superName = "java/lang/Object";
        } else if (clazz.isEnum()) {
            var15 = access | 16416;
        } else {
            var15 = access | 32;
        }

        if (Modifier.isAbstract(modifiers)) {
            var15 |= 1024;
        }

        if (!clazz.isAnnotation()) {
            if (Modifier.isPublic(modifiers)) {
                var15 |= 1;
            } else if (Modifier.isPrivate(modifiers)) {
                var15 |= 2;
            } else if (Modifier.isProtected(modifiers)) {
                var15 |= 4;
            }
        }

        if (Modifier.isFinal(modifiers)) {
            var15 |= 16;
        }

        if (clazz.getSuperclass() != null) {
            superName = Type.getType(clazz.getSuperclass()).getInternalName();
        }

        String[] interfaces = new String[clazz.getInterfaces().length];

        for (int structure = 0; structure < interfaces.length; ++structure) {
            interfaces[structure] = Type.getType(clazz.getInterfaces()[structure]).getInternalName();
        }

        final ClassStructure var16 =
                new ClassStructure(Type.getType(clazz).getInternalName(), var15, superName, interfaces);
        int len$;
        int i$;
        if ((flags & 4) == 4) {
            Annotation[] ex = clazz.getAnnotations();
            if (ex.length > 0) {
                var16.classAnnotations = Maps.newHashMap();
                Annotation[] arr$ = ex;
                len$ = ex.length;

                for (i$ = 0; i$ < len$; ++i$) {
                    Annotation f = arr$[i$];
                    AnnotationDetails field = getAnnotationDetails(f);
                    var16.classAnnotations.put(field.desc, field);
                }
            }
        }

        if (var16.classAnnotations == null) {
            var16.classAnnotations = Collections.emptyMap();
        }

        if (isFieldFlagSet(flags)) {
            var16.fields = Maps.newHashMap();
            Field[] var17 = ClassReflection.getDeclaredFields(clazz);
            Field[] var18 = var17;
            len$ = var17.length;

            for (i$ = 0; i$ < len$; ++i$) {
                Field var19 = var18[i$];
                FieldNode var20 =
                        new FieldNode(0, var19.getName(), Type.getDescriptor(var19.getDeclaringClass()), (String) null,
                                             (Object) null);
                var16.fields.put(var19.getName(), var20);
            }
        } else {
            var16.fields = ImmutableMap.of();
        }

        if (isMethodFlagSet(flags)) {
            var16.methods = Maps.newHashMap();

            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction() {
                    public Object run() throws Exception {
                        java.lang.reflect.Method[] methods = ClassReflection.getDeclaredMethods(clazz);
                        java.lang.reflect.Method[] arr$ = methods;
                        int len$ = methods.length;

                        for (int i$ = 0; i$ < len$; ++i$) {
                            java.lang.reflect.Method m = arr$[i$];
                            var16.methods.put(Method.getMethod(m), ClassStructure.getMethodDetails(m, flags,
                                                                                                          Modifier.isStatic(m.getModifiers())));
                        }

                        return null;
                    }
                });
            } catch (Exception var14) {
                Agent.LOG.log(Level.FINEST, "Error getting methods of " + clazz.getName(), var14);
            }

            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction() {
                    public Object run() throws Exception {
                        Constructor[] constructors = ClassReflection.getDeclaredConstructors(clazz);
                        Constructor[] arr$ = constructors;
                        int len$ = constructors.length;

                        for (int i$ = 0; i$ < len$; ++i$) {
                            Constructor c = arr$[i$];
                            var16.methods.put(Method.getMethod(c), ClassStructure.getMethodDetails(c, flags, false));
                        }

                        return null;
                    }
                });
            } catch (Exception var13) {
                Agent.LOG.log(Level.FINEST, "Error getting constructors of " + clazz.getName(), var13);
            }
        }

        return var16;
    }

    private static boolean isMethodFlagSet(int flags) {
        return (flags & 9) > 0;
    }

    private static boolean isFieldFlagSet(int flags) {
        return (flags & 2) > 0;
    }

    private static ClassStructure.MethodDetails getMethodDetails(AccessibleObject method, int flags, boolean isStatic) {
        if ((flags & 8) != 8) {
            return isStatic ? EMPTY_METHOD_DEFAULTS_STATIC : EMPTY_METHOD_DEFAULTS_MEMBER;
        } else {
            ClassStructure.MethodDetails details =
                    new ClassStructure.MethodDetails(Maps.<String, AnnotationDetails>newHashMap(), isStatic);
            Annotation[] arr$ = method.getAnnotations();
            int len$ = arr$.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                Annotation annotation = arr$[i$];
                AnnotationDetails annotationDetails = getAnnotationDetails(annotation);
                details.annotations.put(annotationDetails.desc, annotationDetails);
            }

            return details;
        }
    }

    private static AnnotationDetails getAnnotationDetails(Annotation annotation) {
        Class annotationType = annotation.annotationType();
        String annotationDesc = Type.getDescriptor(annotationType);
        AnnotationDetails node = new AnnotationDetails(null, annotationDesc);
        java.lang.reflect.Method[] arr$ = annotationType.getDeclaredMethods();
        int len$ = arr$.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            java.lang.reflect.Method annotationMethod = arr$[i$];

            try {
                Object e = annotationMethod.invoke(annotation);
                node.getOrCreateAttributes().put(annotationMethod.getName(), e);
            } catch (Exception var9) {
                Agent.LOG.log(Level.FINEST, "Error getting annotation value for " + annotationMethod.getName(), var9);
            }
        }

        return node;
    }

    public int getAccess() {
        return this.access;
    }

    public String getSuperName() {
        return this.superName;
    }

    public Type getType() {
        return this.type;
    }

    public Set<Method> getMethods() {
        return this.methods.keySet();
    }

    public Map<String, FieldNode> getFields() {
        return this.fields;
    }

    public Map<String, AnnotationDetails> getMethodAnnotations(Method method) {
        ClassStructure.MethodDetails methodDetails = (ClassStructure.MethodDetails) this.methods.get(method);
        return methodDetails == null ? Collections.EMPTY_MAP : methodDetails.annotations;
    }

    public Boolean isStatic(Method method) {
        ClassStructure.MethodDetails methodDetails = (ClassStructure.MethodDetails) this.methods.get(method);
        return methodDetails == null ? null : Boolean.valueOf(methodDetails.isStatic);
    }

    public String[] getInterfaces() {
        return this.interfaces;
    }

    public Map<String, AnnotationDetails> getClassAnnotations() {
        return this.classAnnotations;
    }

    public String toString() {
        return this.type.getClassName();
    }

    private ClassVisitor createClassVisitor(final int flags) {
        ClassVisitor cv = null;
        if (isMethodFlagSet(flags)) {
            cv = new ClassVisitor(Agent.ASM_LEVEL, cv) {
                public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                                 String[] exceptions) {
                    if (null == ClassStructure.this.methods) {
                        ClassStructure.this.methods = Maps.newHashMap();
                    }

                    boolean isStatic = (access & 8) == 8;
                    Method method = new Method(name, desc);
                    if ((flags & 8) == 8) {
                        final ClassStructure.MethodDetails details =
                                new ClassStructure.MethodDetails(Maps.<String, AnnotationDetails>newHashMap(),
                                                                        isStatic);
                        ClassStructure.this.methods.put(method, details);
                        return new MethodVisitor(Agent.ASM_LEVEL,
                                                        super.visitMethod(access, name, desc, signature, exceptions)) {
                            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                                AnnotationDetails annotation =
                                        new AnnotationDetails(super.visitAnnotation(desc, visible), desc);
                                details.annotations.put(desc, annotation);
                                return annotation;
                            }
                        };
                    } else {
                        ClassStructure.this.methods.put(method, isStatic ? ClassStructure.EMPTY_METHOD_DEFAULTS_STATIC
                                                                        : ClassStructure.EMPTY_METHOD_DEFAULTS_MEMBER);
                        return super.visitMethod(access, name, desc, signature, exceptions);
                    }
                }
            };
        }

        if ((flags & 4) == 4) {
            cv = new ClassVisitor(Agent.ASM_LEVEL, cv) {
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    if (null == ClassStructure.this.classAnnotations) {
                        ClassStructure.this.classAnnotations = Maps.newHashMap();
                    }

                    AnnotationDetails annotation = new AnnotationDetails(super.visitAnnotation(desc, visible), desc);
                    ClassStructure.this.classAnnotations.put(desc, annotation);
                    return annotation;
                }
            };
        }

        if (isFieldFlagSet(flags)) {
            cv = new ClassVisitor(Agent.ASM_LEVEL, cv) {
                public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                    FieldNode field = new FieldNode(access, name, desc, signature, value);
                    if (ClassStructure.this.fields == null) {
                        ClassStructure.this.fields = Maps.newHashMap();
                    }

                    ClassStructure.this.fields.put(name, field);
                    return super.visitField(access, name, desc, signature, value);
                }
            };
        }

        return cv;
    }

    private static class MethodDetails {
        final Map<String, AnnotationDetails> annotations;
        final boolean isStatic;

        public MethodDetails(Map<String, AnnotationDetails> annotations, boolean isStatic) {
            this.annotations = annotations;
            this.isStatic = isStatic;
        }
    }
}
