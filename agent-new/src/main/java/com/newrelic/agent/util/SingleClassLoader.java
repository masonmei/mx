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
        Class clazz = (Class) this.classMap.get(classLoader);
        if (clazz == null) {
            clazz = classLoader.loadClass(this.className);
            if (this.classMap.size() == this.maxSize) {
                this.classMap.clear();
            }
            this.classMap.put(classLoader, clazz);
        }
        return clazz;
    }

    public void clear() {
        this.classMap.clear();
    }

    int getSize() {
        return this.classMap.size();
    }
}