package com.newrelic.agent.instrumentation.weaver;

import org.objectweb.asm.commons.Method;

public class MethodWithAccess {
    protected final boolean isStatic;
    protected final Method method;

    public MethodWithAccess(boolean isStatic, Method method) {
        this.method = method;
        this.isStatic = isStatic;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public Method getMethod() {
        return method;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        MethodWithAccess that = (MethodWithAccess) o;

        if (isStatic != that.isStatic) {
            return false;
        }
        if (method != null ? !method.equals(that.method) : that.method != null) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int result = isStatic ? 1 : 0;
        result = 31 * result + (method != null ? method.hashCode() : 0);
        return result;
    }

    public String toString() {
        return isStatic ? "static " + method : method.toString();
    }
}