package com.newrelic.bootstrap;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class BootstrapAgent {
    public static final String AGENT_CLASS_NAME = "com.newrelic.agent.Agent";
    public static final String NEW_RELIC_BOOTSTRAP_CLASSPATH = "newrelic.bootstrap_classpath";
    public static final ClassLoader AGENT_CLASSLOADER = BootstrapAgent.class.getClassLoader();
    private static final String NEW_RELIC_JAR_FILE = "newrelic-jar-with-dependencies.jar";
    private static final String WS_SERVER_JAR = "ws-server.jar";
    private static final String WS_LOG_MANAGER = "com.ibm.ws.kernel.boot.logging.WsLogManager";
    private static final String IBM_VENDOR = "IBM";
    private static long startTime;

    private static JarFile getAgentJarFile(URL agentJarUrl) {
        if (agentJarUrl == null) {
            return null;
        }
        try {
            return new JarFile(URLDecoder.decode(agentJarUrl.getFile().replace("+", "%2B"), "UTF-8"));
        } catch (IOException e) {
        }
        return null;
    }

    public static URL getAgentJarUrl() {
        ClassLoader classLoader = AGENT_CLASSLOADER;
        if ((classLoader instanceof URLClassLoader)) {
            URL[] urls = ((URLClassLoader) classLoader).getURLs();
            for (URL url : urls) {
                if (url.getFile().endsWith(NEW_RELIC_JAR_FILE)) {
                    return url;
                }
            }
            String agentClassName = BootstrapAgent.class.getName().replace('.', '/') + ".class";
            for (URL url : urls) {
                JarFile jarFile = null;
                try {
                    jarFile = new JarFile(url.getFile());
                    ZipEntry entry = jarFile.getEntry(agentClassName);
                    if (entry != null) {
                        return url;
                    }
                } catch (IOException e) {
                } finally {
                    if (jarFile != null) {
                        try {
                            jarFile.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        try {
            Collection<URL> urls = BootstrapLoader.getJarURLs();
            urls.add(getAgentJarUrl());
            ClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), null);
            Class<?> agentClass = classLoader.loadClass(AGENT_CLASS_NAME);
            Method main = agentClass.getDeclaredMethod("main", String[].class);
            main.invoke(null, args);
        } catch (Throwable t) {
            System.err.println(MessageFormat.format("Error invoking the New Relic command: {0}", t));
            t.printStackTrace();
        }
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        String javaVersion = System.getProperty("java.version", "");
        if (javaVersion.startsWith("1.5")) {
            String msg = MessageFormat
                                 .format("Java version is: {0}.  This version of the New Relic Agent does not support"
                                                 + " Java 1.5.  Please use a 2.21.x or earlier version.", javaVersion);

            System.err.println("----------");
            System.err.println(msg);
            System.err.println("----------");
            return;
        }

        checkAndApplyIBMLibertyProfileLogManagerWorkaround();
        startAgent(agentArgs, inst);
    }

    private static void checkAndApplyIBMLibertyProfileLogManagerWorkaround() {
        String javaVendor = System.getProperty("java.vendor");
        if ((javaVendor != null) && (javaVendor.startsWith(IBM_VENDOR))) {
            String javaClassPath = System.getProperty("java.class.path");
            if ((javaClassPath != null) && (javaClassPath.contains(WS_SERVER_JAR)) &&
                        (System.getProperty("java.util.logging.manager") == null)) {
                try {
                    Class.forName(WS_LOG_MANAGER, false, ClassLoader.getSystemClassLoader());

                    System.setProperty("java.util.logging.manager", WS_LOG_MANAGER);
                } catch (Exception e) {
                }
            }
        }
    }

    static void startAgent(String agentArgs, Instrumentation inst) {
        Class<?> clazz;
        if (isBootstrapClasspathFlagSet()) {
            clazz = BootstrapLoader.class;
            clazz = BootstrapLoader.ApiClassTransformer.class;
            appendJarToBootstrapClassLoader(inst);
        }
        try {
            startTime = System.currentTimeMillis();

            BootstrapLoader.load(inst);
            clazz = ClassLoader.getSystemClassLoader().loadClass(AGENT_CLASS_NAME);
            Method premain = clazz.getDeclaredMethod("premain", String.class, Instrumentation.class);
            premain.invoke(null, agentArgs, inst);
        } catch (Throwable t) {
            System.err.println(MessageFormat.format("Error bootstrapping New Relic agent: {0}", t));
            t.printStackTrace();
        }
    }

    public static boolean isBootstrapClasspathFlagSet() {
        return Boolean.getBoolean(NEW_RELIC_BOOTSTRAP_CLASSPATH);
    }

    public static long getAgentStartTime() {
        return startTime;
    }

    private static void appendJarToBootstrapClassLoader(Instrumentation inst) {
        URL agentJarUrl = getAgentJarUrl();
        JarFile agentJarFile = getAgentJarFile(agentJarUrl);
        if (agentJarFile != null) {
            inst.appendToBootstrapClassLoaderSearch(agentJarFile);
        }
    }
}