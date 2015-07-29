package com.newrelic.agent.instrumentation.methodmatchers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

public final class ExactMethodMatcher implements MethodMatcher {
    private final String name;
    private final Set<String> descriptions;
    private final Method[] methods;

    public ExactMethodMatcher(String name, String description) {
        this(name, new String[] {description});
    }

    public ExactMethodMatcher(String name, Collection<String> descriptions) {
        this.name = name;
        if (descriptions.isEmpty()) {
            this.descriptions = Collections.emptySet();
            methods = null;
        } else {
            this.descriptions = Collections.unmodifiableSet(new HashSet(descriptions));
            methods = new Method[descriptions.size()];
            String[] desc = (String[]) descriptions.toArray(new String[0]);
            for (int i = 0; i < desc.length; i++) {
                methods[i] = new Method(name, desc[i]);
            }
        }
    }

    public ExactMethodMatcher(String name, String[] descriptions) {
        this(name, Arrays.asList(descriptions));
    }

    String getName() {
        return name;
    }

    Set<String> getDescriptions() {
        return descriptions;
    }

    public boolean matches(int access, String name, String desc, Set<String> annotations) {
        return (this.name.equals(name)) && ((descriptions.isEmpty()) || (descriptions.contains(desc)));
    }

    public String toString() {
        return "Match " + name + descriptions;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (descriptions == null ? 0 : descriptions.hashCode());
        result = 31 * result + (name == null ? 0 : name.hashCode());
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
        ExactMethodMatcher other = (ExactMethodMatcher) obj;
        if (descriptions == null) {
            if (other.descriptions != null) {
                return false;
            }
        } else if (!descriptions.equals(other.descriptions)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    public void validate() throws InvalidMethodDescriptor {
        boolean valid = true;
        for (String methodDesc : descriptions) {
            try {
                Type[] types = Type.getArgumentTypes(methodDesc);
                for (Type t : types) {
                    if (t == null) {
                        valid = false;
                        break;
                    }
                }
            } catch (Exception e) {
                valid = false;
            }

            if (!valid) {
                throw new InvalidMethodDescriptor("Invalid method descriptor: " + methodDesc);
            }
        }
    }

    public Method[] getExactMethods() {
        return methods;
    }
}