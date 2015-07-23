package com.newrelic.agent.cache;

public final class ClassFieldSignature {
    private final String className;
    private final String fieldName;

    public ClassFieldSignature(String className, String fieldName) {
        this.className = className;
        this.fieldName = fieldName;
    }

    public String getClassName() {
        return className;
    }

    public String getInternalClassName() {
        return className.replace('.', '/');
    }

    public String getFieldName() {
        return fieldName;
    }

    public String toString() {
        return className + '.' + fieldName;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (className == null ? 0 : className.hashCode());
        result = 31 * result + (fieldName == null ? 0 : fieldName.hashCode());
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
        ClassFieldSignature other = (ClassFieldSignature) obj;
        if (className == null) {
            if (other.className != null) {
                return false;
            }
        } else if (!className.equals(other.className)) {
            return false;
        }
        if (fieldName == null) {
            if (other.fieldName != null) {
                return false;
            }
        } else if (!fieldName.equals(other.fieldName)) {
            return false;
        }
        return true;
    }

    public ClassFieldSignature intern() {
        return new ClassFieldSignature(className.intern(), fieldName.intern());
    }
}