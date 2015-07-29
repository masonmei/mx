package com.newrelic.agent.util;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MethodCache
{
  static final int DEFAULT_MAX_SIZE = 100;
  private final Map<Class<?>, Method> methods = new ConcurrentHashMap();
  private final String methodName;
  private final Class<?>[] parameterTypes;
  private final int maxSize;

  public MethodCache(String methodName, Class<?>[] parameterTypes)
  {
    this(100, methodName, parameterTypes);
  }

  public MethodCache(int maxSize, String methodName, Class<?>[] parameterTypes) {
    this.maxSize = maxSize;
    this.methodName = methodName;
    this.parameterTypes = parameterTypes;
  }

  public Method getDeclaredMethod(Class<?> clazz) throws NoSuchMethodException {
    Method method = (Method)this.methods.get(clazz);
    if (method == null) {
      method = clazz.getDeclaredMethod(this.methodName, this.parameterTypes);
      method.setAccessible(true);
      if (this.methods.size() == this.maxSize) {
        this.methods.clear();
      }
      this.methods.put(clazz, method);
    }
    return method;
  }

  public Method getDeclaredMethod(Class<?> clazz, Class<?>[] parameterTypes) throws NoSuchMethodException {
    Method method = (Method)this.methods.get(clazz);
    if (method == null) {
      method = clazz.getDeclaredMethod(this.methodName, parameterTypes);
      method.setAccessible(true);
      if (this.methods.size() == this.maxSize) {
        this.methods.clear();
      }
      this.methods.put(clazz, method);
    }
    return method;
  }

  public Method getMethod(Class<?> clazz) throws NoSuchMethodException {
    Method method = (Method)this.methods.get(clazz);
    if (method == null) {
      method = clazz.getMethod(this.methodName, this.parameterTypes);
      if (this.methods.size() == this.maxSize) {
        this.methods.clear();
      }
      this.methods.put(clazz, method);
    }
    return method;
  }

  public void clear() {
    this.methods.clear();
  }

  int getSize()
  {
    return this.methods.size();
  }
}