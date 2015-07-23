package com.newrelic.agent.instrumentation;

import java.util.HashMap;
import java.util.Map;

public enum NonUrlClassLoaders {
    JBOSS_6(new String[] {"org.jboss.classloader.spi.base.BaseClassLoader"}),

    JBOSS_7(new String[] {"org.jboss.modules.ModuleClassLoader"}),

    WEBSHPERE_8(new String[] {"com.ibm.ws.classloader.CompoundClassLoader"}),

    WEBLOGIC(new String[] {"weblogic.utils.classloaders.GenericClassLoader",
                                  "weblogic.utils.classloaders.ChangeAwareClassLoader"});

    private static Map<String, NonUrlClassLoaders> LOADERS;

    static {
        LOADERS = new HashMap();

        for (NonUrlClassLoaders classLoader : values()) {
            String[] classes = classLoader.classLoaderNames;
            for (String current : classes) {
                LOADERS.put(current, classLoader);
            }
        }
    }

    private String[] classLoaderNames;

    private NonUrlClassLoaders(String[] classNames) {
        classLoaderNames = classNames;
    }

    public static NonUrlClassLoaders getNonUrlType(String loaderCanonicalName) {
        if (loaderCanonicalName != null) {
            return (NonUrlClassLoaders) LOADERS.get(loaderCanonicalName);
        }
        return null;
    }
}