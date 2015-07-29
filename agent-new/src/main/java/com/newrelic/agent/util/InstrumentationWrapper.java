package com.newrelic.agent.util;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.jar.JarFile;
import java.util.logging.Level;

import com.newrelic.agent.Agent;

public class InstrumentationWrapper implements Instrumentation {
    protected final Instrumentation delegate;

    public InstrumentationWrapper(Instrumentation delegate) {
        this.delegate = delegate;
    }

    public void addTransformer(ClassFileTransformer transformer, boolean canRetransform) {
        this.delegate.addTransformer(transformer, canRetransform);
    }

    public void addTransformer(ClassFileTransformer transformer) {
        this.delegate.addTransformer(transformer);
    }

    public boolean removeTransformer(ClassFileTransformer transformer) {
        return this.delegate.removeTransformer(transformer);
    }

    public boolean isRetransformClassesSupported() {
        return this.delegate.isRetransformClassesSupported();
    }

    public void retransformClasses(Class<?>[] classes) throws UnmodifiableClassException {
        if (Agent.LOG.isFinestEnabled()) {
            StringBuilder sb = new StringBuilder("Classes about to be retransformed: ");
            for (Class current : classes) {
                sb.append(current.getName()).append(" ");
            }
            Agent.LOG.log(Level.FINEST, sb.toString());
        }
        this.delegate.retransformClasses(classes);
    }

    public boolean isRedefineClassesSupported() {
        return this.delegate.isRedefineClassesSupported();
    }

    public void redefineClasses(ClassDefinition[] definitions)
            throws ClassNotFoundException, UnmodifiableClassException {
        if (Agent.LOG.isFinestEnabled()) {
            StringBuilder sb = new StringBuilder("Classes about to be redefined: ");
            for (ClassDefinition current : definitions) {
                sb.append(current.getDefinitionClass().getName()).append(" ");
            }
            Agent.LOG.log(Level.FINEST, sb.toString());
        }
        this.delegate.redefineClasses(definitions);
    }

    public boolean isModifiableClass(Class<?> theClass) {
        return this.delegate.isModifiableClass(theClass);
    }

    public Class[] getAllLoadedClasses() {
        return this.delegate.getAllLoadedClasses();
    }

    public Class[] getInitiatedClasses(ClassLoader loader) {
        return this.delegate.getInitiatedClasses(loader);
    }

    public long getObjectSize(Object objectToSize) {
        return this.delegate.getObjectSize(objectToSize);
    }

    public void appendToBootstrapClassLoaderSearch(JarFile jarfile) {
        this.delegate.appendToBootstrapClassLoaderSearch(jarfile);
    }

    public void appendToSystemClassLoaderSearch(JarFile jarfile) {
        this.delegate.appendToSystemClassLoaderSearch(jarfile);
    }

    public boolean isNativeMethodPrefixSupported() {
        return this.delegate.isNativeMethodPrefixSupported();
    }

    public void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {
        this.delegate.setNativeMethodPrefix(transformer, prefix);
    }
}