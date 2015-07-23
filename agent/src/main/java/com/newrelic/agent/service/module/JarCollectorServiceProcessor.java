//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.service.module;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;

import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.ServiceFactory;

class JarCollectorServiceProcessor {
    static final String JAR_EXTENSION = ".jar";
    static final String JAR_PROTOCOL = "jar";
    static final String UNKNOWN_VERSION = " ";
    private static final String SHA1_CHECKSUM_KEY = "sha1Checksum";
    private static final int MAX_MAP_SIZE = 1000;
    private static final JarInfo NON_JAR = new JarInfo((String) null, (Map) null);
    private static final JarInfo JAR_ERROR = new JarInfo((String) null, (Map) null);
    private static final File TEMP_DIRECTORY = new File(System.getProperty("java.io.tmpdir"));
    private final boolean skipTempJars;
    private final List<String> ignoreJars;
    private final Map<URL, JarInfo> sentJars;

    public JarCollectorServiceProcessor() {
        this(ServiceFactory.getConfigService().getDefaultAgentConfig().getIgnoreJars());
    }

    JarCollectorServiceProcessor(List<String> ignoreJars) {
        this.ignoreJars = ignoreJars;
        this.sentJars = CacheBuilder.newBuilder().maximumSize(1000L).weakKeys().<URL, JarInfo>build().asMap();
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        this.skipTempJars =
                ((Boolean) config.getValue("jar_collector.skip_temp_jars", Boolean.valueOf(true))).booleanValue();
        if (!this.skipTempJars) {
            Agent.LOG.finest("Jar collector: temporary jars will be transmitted to the host");
        }

    }

    static boolean isTempFile(URL address) throws URISyntaxException {
        return !"file".equals(address.getProtocol()) ? false : isTempFile(new File(address.toURI()));
    }

    static boolean isTempFile(File file) {
        file = file.getParentFile();
        return null == file ? false : (TEMP_DIRECTORY.equals(file) ? true : isTempFile(file));
    }

    static JarInfo getJarInfoSafe(URL url) {
        String sha1Checksum = "UNKNOWN";

        try {
            sha1Checksum = ShaChecksums.computeSha(url);
        } catch (Exception var5) {
            Agent.LOG.log(Level.FINE, "Error getting jar file checksum : {0}", new Object[] {var5.getMessage()});
            Agent.LOG.log(Level.FINEST, var5, "{0}", new Object[] {var5.getMessage()});
        }

        JarInfo jarInfo;
        try {
            jarInfo = getJarInfo(url, sha1Checksum);
        } catch (Exception var4) {
            Agent.LOG.log(Level.FINEST, var4, "Trouble getting version from {0} jar. Adding jar without version.",
                                 new Object[] {url.getFile()});
            jarInfo = new JarInfo(" ", ImmutableMap.of("sha1Checksum", sha1Checksum));
        }

        return jarInfo;
    }

    private static JarInfo getJarInfo(URL url, String sha1Checksum) throws IOException {
        HashMap attributes = Maps.newHashMap();
        attributes.put("sha1Checksum", sha1Checksum);
        JarInputStream jarFile = EmbeddedJars.getJarInputStream(url);

        try {
            JarInfo var5;
            try {
                Map version = getPom(jarFile);
                if (version != null) {
                    attributes.putAll(version);
                    var5 = new JarInfo((String) version.get("version"), attributes);
                    return var5;
                }
            } catch (Exception var10) {
                Agent.LOG.log(Level.FINEST, var10, "{0}", new Object[] {var10.getMessage()});
            }

            String version1 = getVersion(jarFile);
            if (version1 == null) {
                version1 = " ";
            }

            var5 = new JarInfo(version1, attributes);
            return var5;
        } finally {
            jarFile.close();
        }
    }

    private static Map<Object, Object> getPom(JarInputStream jarFile) throws IOException {
        Properties pom = null;
        JarEntry entry;

        while ((entry = jarFile.getNextJarEntry()) != null) {
            if (entry.getName().startsWith("META-INF/maven") && entry.getName().endsWith("pom.properties")) {
                if (pom != null) {
                    return null;
                }

                Properties props = new Properties();
                props.load(jarFile);
                pom = props;
            }
        }

        return pom;
    }

    static String getVersion(JarInputStream jarFile) {
        Manifest manifest = jarFile.getManifest();
        if (manifest == null) {
            return null;
        } else {
            String version = getVersion(manifest.getMainAttributes());
            if (version == null && !manifest.getEntries().isEmpty()) {
                version = getVersion((Attributes) manifest.getEntries().values().iterator().next());
            }

            return version;
        }
    }

    private static String getVersion(Attributes attributes) {
        String version = attributes.getValue(Name.IMPLEMENTATION_VERSION);
        if (version == null) {
            version = attributes.getValue(Name.SPECIFICATION_VERSION);
            if (version == null) {
                version = attributes.getValue("Bundle-Version");
            }
        }

        return version;
    }

    static String parseJarName(URL url) throws URISyntaxException {
        if ("file".equals(url.getProtocol())) {
            File path1 = new File(url.toURI());
            return path1.getName().trim();
        } else {
            Agent.LOG.log(Level.FINEST, "Parsing jar file name from {0}", new Object[] {url});
            String path = url.getFile();
            int end = path.lastIndexOf(".jar");
            if (end > 0) {
                path = path.substring(0, end);
                int start = path.lastIndexOf(File.separator);
                if (start > 0) {
                    return path.substring(start + 1) + ".jar";
                }
            }

            throw new URISyntaxException(url.getPath(), "Unable to parse the jar file name from a URL");
        }
    }

    protected synchronized List<Jar> processModuleData(Collection<URL> urlsToProcess, boolean sendAll) {
        HashSet urlsToProcess1 = Sets.newHashSet(urlsToProcess);
        ArrayList jars = Lists.newArrayList();
        if (sendAll) {
            urlsToProcess1.addAll(this.sentJars.keySet());
        } else {
            urlsToProcess1.removeAll(this.sentJars.keySet());
        }

        Map processedUrls = this.processUrls(urlsToProcess1, jars);
        this.sentJars.putAll(processedUrls);
        return jars;
    }

    private Map<URL, JarInfo> processUrls(Collection<URL> urls, List<Jar> jars) {
        HashMap jarDetails = Maps.newHashMap();

        URL address;
        JarInfo jar;
        for (Iterator i$ = urls.iterator(); i$.hasNext(); jarDetails.put(address, jar)) {
            address = (URL) i$.next();
            jar = NON_JAR;

            try {
                if (this.skipTempJars && isTempFile(address)) {
                    Agent.LOG.log(Level.FINE, "Skipping temp jar file {0}", new Object[] {address.toString()});
                } else {
                    Agent.LOG.log(Level.FINEST, "Processing jar file {0}", new Object[] {address.toString()});
                    jar = this.processUrl(address, jars);
                }
            } catch (Exception var8) {
                Agent.LOG.log(Level.FINEST, "While processing {0}: {1}: {2}",
                                     new Object[] {address, var8.getClass().getSimpleName(), var8.getMessage()});
            }
        }

        return jarDetails;
    }

    private JarInfo processUrl(URL url, List<Jar> jars) {
        try {
            if (!url.getFile().endsWith(".jar")) {
                return NON_JAR;
            } else {
                Agent.LOG.log(Level.FINEST, "URL has file path {0}.", new Object[] {url.getFile()});
                return this.handleJar(url, jars);
            }
        } catch (Exception var4) {
            Agent.LOG.log(Level.FINEST, var4, "Error processing the file path : {0}", new Object[] {var4.getMessage()});
            return JAR_ERROR;
        }
    }

    private JarInfo handleJar(URL url, List<Jar> jars) {
        JarInfo jarInfo = getJarInfoSafe(url);
        this.addJarAndVersion(url, jarInfo, jars);
        return jarInfo;
    }

    boolean addJarAndVersion(URL url, JarInfo jarInfo, List<Jar> jars) {
        if (jarInfo == null) {
            jarInfo = JarInfo.MISSING;
        }

        boolean added = false;
        String jarFile = null;

        try {
            jarFile = parseJarName(url);
            if (this.shouldAttemptAdd(jarFile)) {
                jars.add(new Jar(jarFile, jarInfo));
                added = true;
            }
        } catch (URISyntaxException var7) {
            Agent.LOG.log(Level.FINEST, var7, "{0}", new Object[] {var7.getMessage()});
        }

        if (added) {
            Agent.LOG.log(Level.FINER, "Adding the jar {0} with version {1}.", new Object[] {jarFile, jarInfo.version});
        } else {
            Agent.LOG.log(Level.FINER, "Not taking version {0} for jar {1}.", new Object[] {jarInfo.version, jarFile});
        }

        return added;
    }

    private boolean shouldAttemptAdd(String jarFile) {
        return !this.ignoreJars.contains(jarFile);
    }
}
