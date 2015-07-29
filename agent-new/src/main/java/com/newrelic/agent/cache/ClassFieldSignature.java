package com.newrelic.agent.cache;

public final class ClassFieldSignature
{
  private final String className;
  private final String fieldName;

  public ClassFieldSignature(String className, String fieldName)
  {
    this.className = className;
    this.fieldName = fieldName;
  }

  public String getClassName() {
    return this.className;
  }

  public String getInternalClassName() {
    return this.className.replace('.', '/');
  }

  public String getFieldName() {
    return this.fieldName;
  }

  public String toString()
  {
    return this.className + '.' + this.fieldName;
  }

  public int hashCode()
  {
    int prime = 31;
    int result = 1;
    result = 31 * result + (this.className == null ? 0 : this.className.hashCode());
    result = 31 * result + (this.fieldName == null ? 0 : this.fieldName.hashCode());
    return result;
  }

  public boolean equals(Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ClassFieldSignature other = (ClassFieldSignature)obj;
    if (this.className == null) {
      if (other.className != null)
        return false;
    }
    else if (!this.className.equals(other.className)) {
      return false;
    }
    if (this.fieldName == null) {
      if (other.fieldName != null)
        return false;
    }
    else if (!this.fieldName.equals(other.fieldName)) {
      return false;
    }
    return true;
  }

  public ClassFieldSignature intern() {
    return new ClassFieldSignature(this.className.intern(), this.fieldName.intern());
  }
}