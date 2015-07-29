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
            this.methods = null;
        } else {
            this.descriptions = Collections.unmodifiableSet(new HashSet(descriptions));
            this.methods = new Method[descriptions.size()];
            String[] desc = (String[]) descriptions.toArray(new String[0]);
            for (int i = 0; i < desc.length; i++) {
                this.methods[i] = new Method(name, desc[i]);
            }
        }
    }

    public ExactMethodMatcher(String name, String[] descriptions) {
        this(name, Arrays.asList(descriptions));
    }

    String getName() {
        return this.name;
    }

    Set<String> getDescriptions() {
        return this.descriptions;
    }

    public boolean matches(int access, String name, String desc, Set<String> annotations) {
        return (this.name.equals(name)) && ((this.descriptions.isEmpty()) || (this.descriptions.contains(desc)));
    }

    public String toString() {
        return "Match " + this.name + this.descriptions;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (this.descriptions == null ? 0 : this.descriptions.hashCode());
        result = 31 * result + (this.name == null ? 0 : this.name.hashCode());
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
        if (this.descriptions == null) {
            if (other.descriptions != null) {
                return false;
            }
        } else if (!this.descriptions.equals(other.descriptions)) {
            return false;
        }
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    public void validate() throws InvalidMethodDescriptor {
        boolean valid = true;
        for (String methodDesc : this.descriptions) {
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
        return this.methods;
    }
}