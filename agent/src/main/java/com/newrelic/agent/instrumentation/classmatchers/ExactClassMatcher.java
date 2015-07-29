package com.newrelic.agent.instrumentation.classmatchers;

import java.util.Arrays;
import java.util.Collection;

import com.newrelic.agent.util.Strings;
import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.Type;

public class ExactClassMatcher extends ClassMatcher {
    private final Type type;
    private final String className;
    private final String internalName;

    public ExactClassMatcher(String className) {
        type = Type.getObjectType(Strings.fixInternalClassName(className));
        this.className = type.getClassName();
        internalName = type.getInternalName();
    }

    public static ClassMatcher or(String[] classNames) {
        return OrClassMatcher.createClassMatcher(classNames);
    }

    public boolean isMatch(ClassLoader loader, ClassReader cr) {
        return cr.getClassName().equals(internalName);
    }

    public boolean isMatch(Class<?> clazz) {
        return clazz.getName().equals(className);
    }

    public String getInternalClassName() {
        return internalName;
    }

    public boolean isExactClassMatcher() {
        return true;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (type == null ? 0 : type.hashCode());
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
        ExactClassMatcher other = (ExactClassMatcher) obj;
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        return true;
    }

    public String toString() {
        return "ExactClassMatcher(" + internalName + ")";
    }

    public Collection<String> getClassNames() {
        return Arrays.asList(new String[] {internalName});
    }
}