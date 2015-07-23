package com.newrelic.agent.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SingleClassLoader {
    static final int DEFAULT_MAX_SIZE = 50;
    private final String className;
    private final int maxSize;
    private Map<ClassLoader, Class<?>> classMap = new ConcurrentHashMap();

    public SingleClassLoader(String className) {
        this(className, 50);
    }

    public SingleClassLoader(String className, int maxSize) {
        this.className = className;
        this.maxSize = maxSize;
    }

    public Class<?> loadClass(ClassLoader classLoader) throws ClassNotFoundException {
        Class clazz = (Class) classMap.get(classLoader);
        if (clazz == null) {
            clazz = classLoader.loadClass(className);
            if (classMap.size() == maxSize) {
                classMap.clear();
            }
            classMap.put(classLoader, clazz);
        }
        return clazz;
    }

    public void clear() {
        classMap.clear();
    }

    int getSize() {
        return classMap.size();
    }
}