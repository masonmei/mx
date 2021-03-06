//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.util.asm;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.bootstrap.EmbeddedJarFilesImpl;
import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.com.google.common.collect.Sets;

public class ClassResolvers {
    private ClassResolvers() {
    }

    public static ClassResolver getEmbeddedJarsClassResolver() {
        ArrayList<ClassResolver> resolvers = Lists.newArrayList();
        String[] embeddedAgentJarFileNames = EmbeddedJarFilesImpl.INSTANCE.getEmbeddedAgentJarFileNames();
        for (String name : embeddedAgentJarFileNames) {
            try {
                resolvers.add(getJarClassResolver(EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(name)));
            } catch (IOException e) {
                Agent.LOG.log(Level.SEVERE, e, "Unable to load {0} : {1}", name, e.getMessage());
            }
        }

        return getMultiResolver(resolvers);
    }

    public static ClassResolver getJarClassResolver(final File jarFile) throws IOException {
        final HashSet classNames = Sets.newHashSet();
        JarFile jar = new JarFile(jarFile);

        try {
            Enumeration e = jar.entries();

            while (e.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) e.nextElement();
                if (jarEntry.getName().endsWith(".class")) {
                    classNames.add(jarEntry.getName());
                }
            }
        } finally {
            jar.close();
        }

        return new ClassResolver() {
            public InputStream getClassResource(String internalClassName) throws IOException {
                String resourceName = internalClassName + ".class";
                if (classNames.contains(resourceName)) {
                    final JarFile jar = new JarFile(jarFile);
                    JarEntry entry = jar.getJarEntry(resourceName);
                    return new BufferedInputStream(jar.getInputStream(entry)) {
                        public void close() throws IOException {
                            super.close();
                            jar.close();
                        }
                    };
                } else {
                    return null;
                }
            }

            public String toString() {
                return jarFile.getAbsolutePath();
            }
        };
    }

    public static ClassResolver getClassLoaderResolver(final ClassLoader classLoader) {
        return new ClassResolver() {
            public InputStream getClassResource(String internalClassName) throws IOException {
                URL resource = Utils.getClassResource(classLoader, internalClassName);
                return resource == null ? null : resource.openStream();
            }
        };
    }

    public static ClassResolver getMultiResolver(final ClassResolver... resolvers) {
        return new ClassResolver() {
            public InputStream getClassResource(String internalClassName) throws IOException {
                if (resolvers != null) {
                    for (ClassResolver resolver : resolvers) {
                        InputStream classResource = resolver.getClassResource(internalClassName);
                        if (classResource != null) {
                            return classResource;
                        }
                    }
                }
                return null;
            }
        };
    }

    public static ClassResolver getMultiResolver(final Collection<ClassResolver> resolvers) {
        return new ClassResolver() {
            public InputStream getClassResource(String internalClassName) throws IOException {
                if (resolvers != null) {
                    for (ClassResolver resolver : resolvers) {
                        InputStream classResource = resolver.getClassResource(internalClassName);
                        if (classResource != null) {
                            return classResource;
                        }
                    }
                }

                return null;
            }
        };
    }
}
