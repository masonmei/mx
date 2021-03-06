package com.newrelic.agent.instrumentation.classmatchers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.newrelic.agent.util.Strings;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.Type;

public class ChildClassMatcher extends ClassMatcher {
    private final String internalSuperClassName;
    private final String superClassName;
    private final boolean onlyMatchChildren;
    private final Set<String> classesToMatch;

    public ChildClassMatcher(String superClassName) {
        this(superClassName, true);
    }

    public ChildClassMatcher(String superClassName, boolean onlyMatchChildren) {
        this(superClassName, onlyMatchChildren, null);
    }

    public ChildClassMatcher(String superClassName, boolean onlyMatchChildren, String[] specificChildClasses) {
        superClassName = Strings.fixInternalClassName(superClassName);
        if (superClassName.indexOf('/') < 0) {
            throw new RuntimeException("Invalid class name format");
        }
        this.superClassName = Type.getObjectType(superClassName).getClassName();
        internalSuperClassName = superClassName;
        this.onlyMatchChildren = onlyMatchChildren;

        classesToMatch = new HashSet();
        classesToMatch.add(internalSuperClassName);
        if (specificChildClasses != null) {
            classesToMatch.addAll(Arrays.asList(specificChildClasses));
        }
    }

    public boolean isMatch(ClassLoader loader, ClassReader cr) {
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        if (cr.getClassName().equals(internalSuperClassName)) {
            if (onlyMatchChildren) {
                return false;
            }
            return true;
        }

        return isSuperMatch(loader, cr.getSuperName());
    }

    private boolean isSuperMatch(ClassLoader loader, String superName) {
        do {
            if (superName.equals(internalSuperClassName)) {
                return true;
            }
            URL resource = Utils.getClassResource(loader, superName);
            if (resource == null) {
                return false;
            }
            try {
                InputStream inputStream = resource.openStream();
                try {
                    ClassReader cr = new ClassReader(inputStream);
                    superName = cr.getSuperName();
                } finally {
                    inputStream.close();
                }
            } catch (IOException ex) {
                return false;
            }
        } while (superName != null);

        return false;
    }

    public boolean isMatch(Class<?> clazz) {
        if (clazz.getName().equals(superClassName)) {
            if (onlyMatchChildren) {
                return false;
            }
            return true;
        }
        while ((clazz = clazz.getSuperclass()) != null) {
            if (clazz.getName().equals(superClassName)) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (internalSuperClassName == null ? 0 : internalSuperClassName.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ChildClassMatcher other = (ChildClassMatcher) obj;
        if (internalSuperClassName == null) {
            if (other.internalSuperClassName != null) {
                return false;
            }
        } else if (!internalSuperClassName.equals(other.internalSuperClassName)) {
            return false;
        }
        return true;
    }

    public Collection<String> getClassNames() {
        return classesToMatch;
    }
}