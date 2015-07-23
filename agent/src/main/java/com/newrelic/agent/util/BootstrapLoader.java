package com.newrelic.agent.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Set;
import java.util.logging.Level;

import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.Agent;
import com.newrelic.agent.util.asm.Utils;

public abstract class BootstrapLoader {
    private static final BootstrapLoader loader = create();

    public static BootstrapLoader get() {
        return loader;
    }

    private static BootstrapLoader create() {
        try {
            return new BootstrapLoaderImpl();
        } catch (Exception e) {
            Agent.LOG.log(Level.FINEST, "IBM sysClassLoader property: {0}",
                                 new Object[] {System.getProperty("systemClassLoader")});
            try {
                return new IBMBootstrapLoader();
            } catch (Exception ex) {
                Agent.LOG.log(Level.FINEST, "IBM Bootstrap loader lookup failed: {0}", new Object[] {ex.getMessage()});

                Agent.LOG.log(Level.FINEST, e, "Error getting bootstrap classloader", new Object[0]);
            }
        }
        return new BootstrapLoader() {
            public URL getBootstrapResource(String name) {
                return null;
            }

            public boolean isBootstrapClass(String internalName) {
                return internalName.startsWith("java/");
            }
        };
    }

    public boolean isBootstrapClass(String internalName) {
        URL bootstrapResource = getBootstrapResource(Utils.getClassResourceName(internalName));
        if (bootstrapResource != null) {
            return true;
        }
        return false;
    }

    public abstract URL getBootstrapResource(String paramString);

    private static class IBMBootstrapLoader extends BootstrapLoader {
        private static final Set<String> BOOTSTRAP_CLASSLOADER_FIELDS =
                ImmutableSet.of("bootstrapClassLoader", "systemClassLoader");
        private final ClassLoader bootstrapLoader;

        public IBMBootstrapLoader() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
            Field field = getBootstrapField();
            field.setAccessible(true);
            ClassLoader cl = (ClassLoader) field.get(null);
            Agent.LOG.log(Level.FINEST, "Initializing IBM BootstrapLoader");
            bootstrapLoader = cl;
        }

        private Field getBootstrapField() throws NoSuchFieldException {
            for (String fieldName : BOOTSTRAP_CLASSLOADER_FIELDS) {
                try {
                    Agent.LOG.log(Level.FINEST, "Searching for java.lang.ClassLoader.{0}", new Object[] {fieldName});
                    return ClassLoader.class.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                } catch (SecurityException e) {
                }
            }
            throw new NoSuchFieldException(MessageFormat.format("No bootstrap fields found: {0}",
                                                                       new Object[] {BOOTSTRAP_CLASSLOADER_FIELDS}));
        }

        public URL getBootstrapResource(String name) {
            return bootstrapLoader.getResource(name);
        }
    }

    private static class BootstrapLoaderImpl extends BootstrapLoader {
        private final Method getBootstrapResourceMethod;

        private BootstrapLoaderImpl()
                throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
                               InvocationTargetException {
            getBootstrapResourceMethod =
                    ClassLoader.class.getDeclaredMethod("getBootstrapResource", new Class[] {String.class});
            getBootstrapResourceMethod.setAccessible(true);
            getBootstrapResourceMethod.invoke(null, new Object[] {"dummy"});
        }

        public URL getBootstrapResource(String name) {
            try {
                return (URL) getBootstrapResourceMethod.invoke(null, new Object[] {name});
            } catch (Exception e) {
                Agent.LOG.log(Level.FINEST, e.toString(), e);
            }
            return null;
        }
    }
}