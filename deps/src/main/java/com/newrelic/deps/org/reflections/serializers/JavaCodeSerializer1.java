package com.newrelic.deps.org.reflections.serializers;

import com.newrelic.deps.com.google.common.base.Joiner;
import com.newrelic.deps.com.google.common.base.Supplier;
import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.com.google.common.collect.Multimap;
import com.newrelic.deps.com.google.common.collect.Multimaps;
import com.newrelic.deps.com.google.common.collect.Sets;
import com.newrelic.deps.com.google.common.io.Files;
import com.newrelic.deps.org.reflections.ReflectionUtils;
import com.newrelic.deps.org.reflections.Reflections;
import com.newrelic.deps.org.reflections.ReflectionsException;
import com.newrelic.deps.org.reflections.scanners.TypeElementsScanner;
import com.newrelic.deps.org.reflections.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.*;

import static com.newrelic.deps.org.reflections.Reflections.log;
import static com.newrelic.deps.org.reflections.util.Utils.prepareFile;
import static com.newrelic.deps.org.reflections.util.Utils.repeat;

/**
 * serializes types and elements into interfaces respectively to fully qualified name,
 * <p> for example:
 * <pre>
 * public interface MyTestModelStore {
 *	public interface <b>org</b> extends IPackage {
 *	    public interface <b>reflections</b> extends IPackage {
 *			public interface <b>TestModel$AC1</b> extends IClass {}
 *			public interface <b>TestModel$C4</b> extends IClass {
 *				public interface <b>f1</b> extends IField {}
 *				public interface <b>m1</b> extends IMethod {}
 *				public interface <b>m1_int_java$lang$String$$$$</b> extends IMethod {}
 *	...
 * }
 * </pre>
 * <p> use the different resolve methods to resolve the serialized element into Class, Field or Method. for example:
 * <pre>
 *  Class&#60? extends IMethod> imethod = MyTestModelStore.org.reflections.TestModel$C4.m1.class;
 *  Method method = JavaCodeSerializer.resolve(imethod);
 * </pre>
 * <p>depends on Reflections configured with {@link org.reflections.scanners.TypeElementsScanner}
 * <p><p>the {@link #save(org.reflections.Reflections, String)} method filename should be in the pattern: path/path/path/package.package.classname
 * */
public class JavaCodeSerializer1 implements Serializer {

    private static final String pathSeparator = "_";
    private static final String doubleSeparator = "__";
    private static final String dotSeparator = ".";
    private static final String arrayDescriptor = "$$";
    private static final String tokenSeparator = "_";

    public Reflections read(InputStream inputStream) {
        throw new UnsupportedOperationException("read is not implemented on JavaCodeSerializer");
    }

    /**
     * name should be in the pattern: path/path/path/package.package.classname,
     * for example <pre>/data/projects/my/src/main/java/org.my.project.MyStore</pre>
     * would create class MyStore in package org.my.project in the path /data/projects/my/src/main/java
     */
    public File save(Reflections reflections, String name) {
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1); //trim / at the end
        }

        //prepare file
        String filename = name.replace('.', '/').concat(".java");
        File file = prepareFile(filename);

        //get package and class names
        String packageName;
        String className;
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
            packageName = "";
            className = name.substring(name.lastIndexOf('/') + 1);
        } else {
            packageName = name.substring(name.lastIndexOf('/') + 1, lastDot);
            className = name.substring(lastDot + 1);
        }

        //generate
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("//generated using Reflections JavaCodeSerializer")
                    .append(" [").append(new Date()).append("]")
                    .append("\n");
            if (packageName.length() != 0) {
                sb.append("package ").append(packageName).append(";\n");
                sb.append("\n");
            }
            sb.append("public interface ").append(className).append(" {\n\n");
            sb.append(toString(reflections));
            sb.append("}\n");

            Files.write(sb.toString(), new File(filename), Charset.defaultCharset());

        } catch (IOException e) {
            throw new RuntimeException();
        }

        return file;
    }

    private static class Writer {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        public Writer append(String s) { sb.append(s); return this; }

        public Writer indentRight() { return append(repeat("\t", ++indent)); }
        public Writer indentLeft() { return append(repeat("\t", --indent)); }
        public Writer indent() { return append(repeat("\t", indent)); }

        @Override public String toString() { return sb.toString(); }
    }
    
    public String toString(Reflections reflections) {
        if (reflections.getStore().get(TypeElementsScanner.class.getSimpleName()).isEmpty()) {
            if (log != null) log.warn("JavaCodeSerializer needs TypeElementsScanner configured");
        }

        Writer sb = new Writer();

        String prev = "";
        boolean prevEmpty = false;

        List<String> keys = Lists.newArrayList(reflections.getStore().get(TypeElementsScanner.class.getSimpleName()).keySet());
        Collections.sort(keys);
        for (String key : keys) {
            String fqn = key.replace("$", ".");
            String common = commonPath(prev, fqn);
            String cn = fqn.contains(".") ? fqn.substring(fqn.lastIndexOf(".") + 1) : "";

            if (prev.length() > common.length() + 1) {
                for (String ignored : prev.substring(common.length() + 1).split("\\.")) {
                    if (!prevEmpty) sb.indentLeft(); else prevEmpty = false;
                    sb.append("}\n");
                }
            }

            if (fqn.length() > common.length()) {
                String[] split = fqn.substring(common.length()).split("\\.");
                for (int i = 0; i < split.length - 1; i++) {
                    String path = split[i];
                    if (path.length() > 0) {
                        sb.indentRight().append("public interface ").append(path).append(" {\n");
                    } else {
                        --sb.indent;
                    }
                }
            }

            Data data = new Data(reflections, key);

            //add class
            sb.indentRight().append("public enum ").append(cn).append(" {");
            if (!data.elements.isEmpty()) {
                sb.append("\n");
                ++sb.indent;
                int fs = data.fields.size();
                int ms = data.elements.size() - fs;
                int ss = fs + ms;
                if (fs > 0) {
                    sb.indent().append("//fields").append("\n");
                    for (int i = 0; i < fs; i++) {
                        sb.indent().append(data.elements.get(i)).append(i < ss - 1 ? "," : "").append("\n");
                    }
                }
                if (ms > 0) {
                    sb.indent().append("//methods").append("\n");
                    for (int i = fs; i < ss; i++) {
                        sb.indent().append(data.elements.get(i)).append(i < ss - 1 ? "," : "").append("\n");
                    }
                }
            }

            prevEmpty = data.elements.isEmpty();
            prev = fqn;
        }

        for (String ignored : prev.split("\\.")) {
            sb.indentLeft().append("}\n");
        }

        return sb.toString();
    }

    private String commonPath(String prev, String fqn) {
        int maxPrefixLength = Math.min(prev.length(), fqn.length());
        int p = 0;
        while (p < maxPrefixLength && prev.charAt(p) == fqn.charAt(p)) {
          p++;
        }
        if (aBoolean(prev, p) || aBoolean(fqn, p)) {
          p = prev.lastIndexOf(".");
        }
        return prev.subSequence(0, p).toString();
    }

    private boolean aBoolean(String prev, int p) {
        return p - 1 >= 0 && p - 1 <= (prev.length() - 1) && prev.charAt(p - 1) != '.';
    }

    private String name(String candidate, List<String> prev, int offset) {
        String normalized = candidate.replace(dotSeparator, pathSeparator);
        for (int i = 0; i < offset; i++) {
            if (normalized.equals(prev.get(i))) {
                return name(normalized + tokenSeparator, prev, offset);
            }
        }

        return !normalized.contains("$") ? normalized : Joiner.on(dotSeparator).join(normalized.split("\\$"));
    }

    //
    public static Class<?> resolveClassOf(final Class element) throws ClassNotFoundException {
        Class<?> cursor = element;
        LinkedList<String> ognl = Lists.newLinkedList();

        while (cursor != null) {
            ognl.addFirst(cursor.getSimpleName());
            cursor = cursor.getDeclaringClass();
        }

        String classOgnl = Joiner.on(".").join(ognl.subList(1, ognl.size())).replace(".$", "$");
        return Class.forName(classOgnl);
    }

    public static Class<?> resolveClass(final Class aClass) {
        try {
            return resolveClassOf(aClass);
        } catch (Exception e) {
            throw new ReflectionsException("could not resolve to class " + aClass.getName(), e);
        }
    }

    public static Field resolveField(final Class aField) {
        try {
            String name = aField.getSimpleName();
            Class<?> declaringClass = aField.getDeclaringClass().getDeclaringClass();
            return resolveClassOf(declaringClass).getDeclaredField(name);
        } catch (Exception e) {
            throw new ReflectionsException("could not resolve to field " + aField.getName(), e);
        }
    }

    public static Annotation resolveAnnotation(Class annotation) {
        try {
            String name = annotation.getSimpleName().replace(pathSeparator, dotSeparator);
            Class<?> declaringClass = annotation.getDeclaringClass().getDeclaringClass();
            Class<?> aClass = resolveClassOf(declaringClass);
            //noinspection unchecked
            Class<? extends Annotation> aClass1 = (Class<? extends Annotation>) ReflectionUtils.forName(name);
            return aClass.getAnnotation(aClass1);
        } catch (Exception e) {
            throw new ReflectionsException("could not resolve to annotation " + annotation.getName(), e);
        }
    }

    public static Method resolveMethod(final Class aMethod) {
        String methodOgnl = aMethod.getSimpleName();

        try {
            String methodName;
            Class<?>[] paramTypes;
            if (methodOgnl.contains(tokenSeparator)) {
                methodName = methodOgnl.substring(0, methodOgnl.indexOf(tokenSeparator));
                String[] params = methodOgnl.substring(methodOgnl.indexOf(tokenSeparator) + 1).split(doubleSeparator);
                paramTypes = new Class<?>[params.length];
                for (int i = 0; i < params.length; i++) {
                    String typeName = params[i].replace(arrayDescriptor, "[]").replace(pathSeparator, dotSeparator);
                    paramTypes[i] = ReflectionUtils.forName(typeName);
                }
            } else {
                methodName = methodOgnl;
                paramTypes = null;
            }

            Class<?> declaringClass = aMethod.getDeclaringClass().getDeclaringClass();
            return resolveClassOf(declaringClass).getDeclaredMethod(methodName, paramTypes);
        } catch (Exception e) {
            throw new ReflectionsException("could not resolve to method " + aMethod.getName(), e);
        }
    }

    private class Data {
        private List<String> annotations;
        private List<String> fields;
        private Multimap<String, String> methods;
        private List<String> elements;

        public Data(Reflections reflections, String key) {
            //get fields and methods
            annotations = Lists.newArrayList();
            fields = Lists.newArrayList();
            methods = Multimaps.newSetMultimap(new HashMap<String, Collection<String>>(), new Supplier<Set<String>>() {
                public Set<String> get() {
                    return Sets.newHashSet();
                }
            });

            for (String element : reflections.getStore().get(TypeElementsScanner.class.getSimpleName(), key)) {
                if (element.startsWith("@")) {
                    annotations.add(element.substring(1));
                } else if (element.contains("(")) {
                    //method
                    if (!element.startsWith("<")) {
                        int i1 = element.indexOf('(');
                        String name = element.substring(0, i1);
                        String params = element.substring(i1 + 1, element.indexOf(")"));

                        String paramsDescriptor = "";
                        if (params.length() != 0) {
                            paramsDescriptor = tokenSeparator + params.replace(dotSeparator, tokenSeparator).
                                    replace(", ", doubleSeparator).replace("[]", arrayDescriptor);
                        }
                        String normalized = name + paramsDescriptor;
                        methods.put(name, normalized);
                    }
                } else if (!Utils.isEmpty(element)) {
                    //field
                    fields.add(element);
                }
            }

            elements = Lists.newArrayList();
            elements.addAll(fields);
            for (String k : methods.keySet()) {
                if (!elements.contains(k)) {
                    elements.add(k);
                } else {
                    List<String> strings = Lists.newArrayList(methods.get(k));
                    Collections.sort(strings);
                    for (String string : strings) {
                        elements.add(name(string, elements, 0));
                    }
                }

            }
            for (Map.Entry<String, String> entry : methods.entries()) {
            }
        }
    }
}
