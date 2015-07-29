package com.newrelic.agent.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import com.newrelic.agent.Agent;
import com.newrelic.deps.org.objectweb.asm.Type;

public class AgentJarHelper {
    private static final Pattern AGENT_CLASS_PATTERN = Pattern.compile(Type.getInternalName(Agent.class) + ".class");
    private static final String AGENT_CLASS_NAME = Type.getInternalName(Agent.class) + ".class";
    //    private static final String NEW_RELIC_JAR_FILE = "newrelic.jar";
    private static final String NEW_RELIC_JAR_FILE = "newrelic-jar-with-dependencies.jar";
    private static final String BUILT_DATE_ATTRIBUTE = "Built-Date";

    public static Collection<String> findAgentJarFileNames(Pattern pattern) {
        URL agentJarUrl = getAgentJarUrl();
        Agent.LOG.log(Level.FINEST, "Searching for " + pattern.pattern() + " in " + agentJarUrl.getPath());
        return findJarFileNames(agentJarUrl, pattern);
    }

    public static Collection<String> findJarFileNames(URL agentJarUrl, Pattern pattern) {
        JarFile jarFile = null;
        try {
            jarFile = getAgentJarFile(agentJarUrl);

            Collection names = new ArrayList();
            for (Enumeration entries = jarFile.entries(); entries.hasMoreElements(); ) {
                JarEntry jarEntry = (JarEntry) entries.nextElement();
                if (pattern.matcher(jarEntry.getName()).matches()) {
                    names.add(jarEntry.getName());
                }
            }
            return names;
        } catch (Exception e) {
            Agent.LOG.log(Level.FINEST, "Unable to search the agent jar for " + pattern.pattern(), e);
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                }
            }
        }
        return Collections.emptyList();
    }

    public static boolean jarFileNameExists(URL agentJarUrl, String name) {
        JarFile jarFile = null;
        try {
            jarFile = getAgentJarFile(agentJarUrl);
            return jarFile.getEntry(name) != null;
        } catch (Exception e) {
            Agent.LOG.log(Level.FINEST, "Unable to search the agent jar for " + name, e);
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                }
            }
        }
        return false;
    }

    public static File getAgentJarDirectory() {
        URL agentJarUrl = getAgentJarUrl();
        if (agentJarUrl != null) {
            File file = new File(getAgentJarFileName(agentJarUrl));
            if (file.exists()) {
                return file.getParentFile();
            }
        }
        return null;
    }

    public static URL getAgentJarUrl() {
        if (System.getProperty("newrelic.agent_jarfile") != null) {
            try {
                return new URL("file://" + System.getProperty("newrelic.agent_jarfile"));
            } catch (MalformedURLException e) {
                Agent.LOG.log(Level.FINEST, "Unable to create a valid url from "
                                                    + System.getProperty("newrelic.agent_jarfile"), e);
            }

        }

        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        if ((classLoader instanceof URLClassLoader)) {
            URL[] urls = ((URLClassLoader) classLoader).getURLs();
            for (URL url : urls) {
                if ((url.getFile().endsWith(NEW_RELIC_JAR_FILE)) && (jarFileNameExists(url, AGENT_CLASS_NAME))) {
                    return url;
                }
            }

            String agentClassName = Agent.class.getName().replace('.', '/') + ".class";
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

        return AgentJarHelper.class.getProtectionDomain().getCodeSource().getLocation();
    }

    public static JarResource getAgentJarResource() {
        final JarFile agentJarFile = getAgentJarFile();
        if (agentJarFile == null) {
            return new JarResource() {
                public void close() throws IOException {
                }

                public InputStream getInputStream(String name) {
                    return AgentJarHelper.class.getResourceAsStream('/' + name);
                }

                public long getSize(String name) {
                    return 128L;
                }
            };
        }
        return new JarResource() {
            public void close() throws IOException {
                agentJarFile.close();
            }

            public InputStream getInputStream(String name) throws IOException {
                ZipEntry entry = agentJarFile.getJarEntry(name);
                return agentJarFile.getInputStream(entry);
            }

            public long getSize(String name) {
                ZipEntry entry = agentJarFile.getJarEntry(name);
                return entry.getSize();
            }
        };
    }

    private static JarFile getAgentJarFile() {
        URL agentJarUrl = getAgentJarUrl();
        return getAgentJarFile(agentJarUrl);
    }

    private static JarFile getAgentJarFile(URL agentJarUrl) {
        if (agentJarUrl == null) {
            return null;
        }
        try {
            return new JarFile(getAgentJarFileName(agentJarUrl));
        } catch (IOException e) {
        }
        return null;
    }

    private static String getAgentJarFileName(URL agentJarUrl) {
        if (agentJarUrl == null) {
            return null;
        }
        try {
            return URLDecoder.decode(agentJarUrl.getFile().replace("+", "%2B"), "UTF-8");
        } catch (IOException e) {
        }
        return null;
    }

    public static String getAgentJarFileName() {
        URL agentJarUrl = getAgentJarUrl();
        return getAgentJarFileName(agentJarUrl);
    }

    public static String getBuildDate() {
        return getAgentJarAttribute(BUILT_DATE_ATTRIBUTE);
    }

    public static String getAgentJarAttribute(String name) {
        JarFile jarFile = getAgentJarFile();
        if (jarFile == null) {
            return null;
        }
        try {
            return jarFile.getManifest().getMainAttributes().getValue(name);
        } catch (IOException e) {
        }
        return null;
    }
}