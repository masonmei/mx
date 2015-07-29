package com.newrelic.agent.instrumentation.verifier.mocks;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.jar.JarFile;

public class MockInstrumentation implements Instrumentation {
    public void addTransformer(ClassFileTransformer arg0) {
    }

    public void addTransformer(ClassFileTransformer arg0, boolean arg1) {
    }

    public void appendToBootstrapClassLoaderSearch(JarFile arg0) {
    }

    public void appendToSystemClassLoaderSearch(JarFile arg0) {
    }

    public Class[] getAllLoadedClasses() {
        return new Class[0];
    }

    public Class[] getInitiatedClasses(ClassLoader arg0) {
        return null;
    }

    public long getObjectSize(Object arg0) {
        return 0L;
    }

    public boolean isModifiableClass(Class<?> arg0) {
        return false;
    }

    public boolean isNativeMethodPrefixSupported() {
        return false;
    }

    public boolean isRedefineClassesSupported() {
        return false;
    }

    public boolean isRetransformClassesSupported() {
        return false;
    }

    public void redefineClasses(ClassDefinition[] arg0) throws ClassNotFoundException, UnmodifiableClassException {
    }

    public boolean removeTransformer(ClassFileTransformer arg0) {
        return false;
    }

    public void retransformClasses(Class<?>[] arg0) throws UnmodifiableClassException {
    }

    public void setNativeMethodPrefix(ClassFileTransformer arg0, String arg1) {
    }
}