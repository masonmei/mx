package com.newrelic.agent.instrumentation.verifier;

import java.net.URL;
import java.net.URLClassLoader;

public class VerificationClassLoader extends URLClassLoader {
    public VerificationClassLoader(URL[] urls) {
        super(urls);
    }

    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith("java.")) {
            return super.loadClass(name, resolve);
        }
        Class c = findClass(name);
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }
}