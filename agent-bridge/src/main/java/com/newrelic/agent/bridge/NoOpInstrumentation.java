package com.newrelic.agent.bridge;

import java.io.Closeable;
import java.lang.reflect.Method;

public class NoOpInstrumentation implements Instrumentation {
    public ExitTracer createTracer(Object invocationTarget, int signatureId, String metricName, int flags) {
        return null;
    }

    public void noticeInstrumentationError(Throwable throwable, String libraryName) {
    }

    public void instrument(String className, String metricPrefix) {
    }

    public void instrument(Method methodToInstrument, String metricPrefix) {
    }

    public void retransformUninstrumentedClass(Class<?> classToRetransform) {
    }

    public Class<?> loadClass(ClassLoader classLoader, Class<?> theClass) throws ClassNotFoundException {
        return null;
    }

    public Transaction getTransaction() {
        return NoOpTransaction.INSTANCE;
    }

    public int addToObjectCache(Object object) {
        return -1;
    }

    public Object getCachedObject(int id) {
        return null;
    }

    public boolean isWeaveClass(Class<?> clazz) {
        return false;
    }

    public void registerCloseable(String string, Closeable closeable) {
    }

    public ExitTracer createTracer(Object invocationTarget, int signatureId, boolean dispatcher, String metricName,
                                   String tracerFactoryName, Object[] args) {
        return null;
    }
}