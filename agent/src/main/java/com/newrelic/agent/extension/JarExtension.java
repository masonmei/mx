package com.newrelic.agent.extension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.regex.Pattern;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.weaver.WeaveUtils;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.Streams;
import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.org.objectweb.asm.ClassReader;

public class JarExtension {
    private final ClassLoader classloader;
    private final File file;
    private final Map<String, Extension> extensions = new HashMap<String, Extension>();

    private JarExtension(IAgentLogger logger, ExtensionParsers extensionParsers, File file, ClassLoader classLoader,
                         boolean custom) throws IOException {
        classloader = classLoader;
        this.file = file;
        JarFile jarFile = new JarFile(file);

        logger.fine(MessageFormat
                            .format(!custom ? "Loading built-in agent extensions" : "Loading extension jar \"{0}\"",
                                           file.getAbsolutePath()));

        Collection<JarEntry> entries = getExtensions(jarFile);
        for (JarEntry entry : entries) {
            InputStream iStream = null;
            try {
                iStream = jarFile.getInputStream(entry);
                if (iStream != null) {
                    try {
                        Extension extension =
                                extensionParsers.getParser(entry.getName()).parse(classLoader, iStream, custom);

                        addExtension(extension);
                    } catch (Exception ex) {
                        logger.severe(MessageFormat.format("Invalid extension file {0} : {1}", entry.getName(),
                                                                  ex.toString()));

                        logger.log(Level.FINER, ex.toString(), ex);
                    }
                } else {
                    logger.fine(MessageFormat.format("Unable to load extension resource \"{0}\"", entry.getName()));
                }
            } finally {
                if (iStream != null) {
                    try {
                        iStream.close();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    public static JarExtension create(IAgentLogger logger, ExtensionParsers extensionParsers, File file)
            throws IOException {
        JarFile jar = new JarFile(file);
        String agentClass;
        try {
            agentClass = getAgentClass(jar.getManifest());
            if (null != agentClass) {
                logger.log(Level.FINE, "Detected agentmain class {0} in {1}", agentClass, file.getAbsolutePath());

                byte[] newBytes = ExtensionRewriter.rewrite(jar);
                if (null != newBytes) {
                    validateJar(newBytes);
                    file = writeTempJar(logger, file, newBytes);
                }
            }
        } finally {
            jar.close();
        }

        JarExtension ext = new JarExtension(logger, extensionParsers, file,
                                                   new URLClassLoader(new URL[] {file.toURI().toURL()},
                                                                             ClassLoader.getSystemClassLoader()), true);

        if (agentClass != null) {
            ext.invokeMainMethod(logger, agentClass);
        }

        return ext;
    }

    public static JarExtension create(IAgentLogger logger, ExtensionParsers extensionParsers, String jarFileName)
            throws IOException {
        return new JarExtension(logger, extensionParsers, new File(jarFileName), ClassLoader.getSystemClassLoader(),
                                       false);
    }

    private static Collection<JarEntry> getExtensions(JarFile file) {
        List<JarEntry> list = new ArrayList<JarEntry>();
        Pattern pattern = Pattern.compile("^META-INF/extensions/(.*).(yml|xml)$");

        for (Enumeration entries = file.entries(); entries.hasMoreElements(); ) {
            JarEntry entry = (JarEntry) entries.nextElement();
            String name = entry.getName();
            if (pattern.matcher(name).matches()) {
                list.add(entry);
            }
        }
        return list;
    }

    public static boolean isWeaveInstrumentation(File file) {
        Collection<String> classNames = getClassFileNames(file);

        if (!classNames.isEmpty()) {
            if (!file.exists()) {
                return false;
            }

            JarFile jarFile = null;
            try {
                jarFile = new JarFile(file);
                for (String fileName : classNames) {
                    JarEntry jarEntry = jarFile.getJarEntry(fileName);
                    InputStream stream = null;
                    try {
                        stream = jarFile.getInputStream(jarEntry);
                        if (stream != null) {
                            ClassReader reader = new ClassReader(stream);

                            if (WeaveUtils.isWeavedClass(reader)) {
                                return true;
                            }
                        }
                    } catch (IOException e) {
                        Agent.LOG.log(Level.INFO, "Error processing " + fileName, e);
                    } finally {
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (IOException e) {
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                Agent.LOG.log(Level.INFO, "Error processing extension jar " + file, ex);
            } finally {
                if (jarFile != null) {
                    try {
                        jarFile.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        return false;
    }

    public static Collection<String> getClassFileNames(File file) {
        if (file.exists()) {
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(file);

                Collection<String> classes = new ArrayList<String>();
                Enumeration entries = jarFile.entries();
                JarEntry entry;
                while (entries.hasMoreElements()) {
                    entry = (JarEntry) entries.nextElement();
                    if ((!entry.isDirectory()) && (entry.getName().endsWith(".class"))) {
                        String fileName = entry.getName();
                        try {
                            classes.add(fileName);
                        } catch (Exception ex) {
                        }
                    }
                }
                return classes;
            } catch (IOException e) {
                Agent.LOG.debug("Unable to read classes in " + file.getAbsolutePath() + ".  " + e.getMessage());
            } finally {
                if (jarFile != null) {
                    try {
                        jarFile.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private static String getAgentClass(Manifest manifest) {
        for (String attr : Arrays.asList(new String[] {"Agent-Class", "Premain-Class"})) {
            String agentClass = manifest.getMainAttributes().getValue(attr);
            if (null != agentClass) {
                return agentClass;
            }
        }
        return null;
    }

    private static File writeTempJar(IAgentLogger logger, File file, byte[] newBytes) throws IOException {
        File original = file;
        file = File.createTempFile(file.getName(), ".jar");
        file.deleteOnExit();

        FileOutputStream out = new FileOutputStream(file);
        try {
            Streams.copy(new ByteArrayInputStream(newBytes), out, newBytes.length);
        } finally {
            out.close();
        }
        logger.log(Level.FINER, "Rewriting {0} as {1}", original.getAbsolutePath(), file.getAbsolutePath());

        return file;
    }

    private static void validateJar(byte[] bytes) throws IOException {
    }

    public ClassLoader getClassloader() {
        return classloader;
    }

    public final Map<String, Extension> getExtensions() {
        return Collections.unmodifiableMap(extensions);
    }

    void addExtension(Extension extension) {
        Extension existing = extensions.get(extension.getName());
        if ((existing == null) || (existing.getVersionNumber() < extension.getVersionNumber())) {
            extensions.put(extension.getName(), extension);
        }
    }

    public boolean isWeaveInstrumentation() {
        return isWeaveInstrumentation(file);
    }

    public Collection<String> getClassFileNames() {
        return getClassFileNames(file);
    }

    public Collection<Class<?>> getClasses() {
        Collection<String> classNames = getClassFileNames();
        if (classNames.isEmpty()) {
            return Collections.emptyList();
        }

        Collection<Class<?>> classes = Lists.newArrayList();
        for (String fileName : classNames) {
            int index = fileName.indexOf(".class");
            fileName = fileName.substring(0, index);
            fileName = fileName.replace('/', '.');
            try {
                classes.add(classloader.loadClass(fileName));
            } catch (Exception ex) {
            }
        }
        return classes;
    }

    public File getFile() {
        return file;
    }

    public String toString() {
        return file.getAbsolutePath();
    }

    private void invokeMainMethod(IAgentLogger logger, String agentClass) {
        try {
            Class<?> clazz = classloader.loadClass(agentClass);
            logger.log(Level.FINE, "Invoking {0}.premain method", agentClass);
            Method method = clazz.getDeclaredMethod("premain", String.class, Instrumentation.class);
            String agentArgs = "";
            method.invoke(null, agentArgs, ServiceFactory.getClassTransformerService().getExtensionInstrumentation());
        } catch (ClassNotFoundException e) {
            logger.log(Level.INFO, "Unable to load {0}", agentClass);
            logger.log(Level.FINEST, e, e.getMessage());
        } catch (NoSuchMethodException e) {
            logger.log(Level.INFO, "{0} has no premain method", agentClass);
            logger.log(Level.FINEST, e, e.getMessage());
        } catch (SecurityException e) {
            logger.log(Level.INFO, "Unable to load {0}", agentClass);
            logger.log(Level.FINEST, e, e.getMessage());
        } catch (Exception e) {
            logger.log(Level.INFO, "Unable to invoke {0}.premain", agentClass);
            logger.log(Level.FINEST, e, e.getMessage());
        }
    }
}