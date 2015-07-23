package com.newrelic.agent.bridge;

import java.io.Closeable;
import java.lang.reflect.Method;

public interface Instrumentation {
    ExitTracer createTracer(Object paramObject, int paramInt1, String paramString, int paramInt2);

    ExitTracer createTracer(Object paramObject, int paramInt, boolean paramBoolean, String paramString1,
                            String paramString2, Object[] paramArrayOfObject);

    Transaction getTransaction();

    void noticeInstrumentationError(Throwable paramThrowable, String paramString);

    void instrument(String paramString1, String paramString2);

    void instrument(Method paramMethod, String paramString);

    void retransformUninstrumentedClass(Class<?> paramClass);

    Class<?> loadClass(ClassLoader paramClassLoader, Class<?> paramClass) throws ClassNotFoundException;

    int addToObjectCache(Object paramObject);

    Object getCachedObject(int paramInt);

    boolean isWeaveClass(Class<?> paramClass);

    void registerCloseable(String paramString, Closeable paramCloseable);
}