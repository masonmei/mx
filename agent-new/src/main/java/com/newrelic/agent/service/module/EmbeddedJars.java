package com.newrelic.agent.service.module;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class EmbeddedJars {
    private static final String EMBEDDED_JAR = ".jar!/";

    public static InputStream getInputStream(URL url) throws IOException {
        int index = url.toExternalForm().indexOf(".jar!/");
        InputStream inputStream;
        if (index > 0) {
            String path = url.toExternalForm().substring(index + ".jar!/".length());

            url = new URL(url.toExternalForm().substring(0, index) + ".jar");
            inputStream = url.openStream();
            JarInputStream jarStream = new JarInputStream(inputStream);
            if (!readToEntry(jarStream, path)) {
                inputStream.close();
                throw new IOException("Unable to open stream for " + path + " in " + url.toExternalForm());
            }
            inputStream = jarStream;
        } else {
            inputStream = url.openStream();
        }

        return inputStream;
    }

    private static boolean readToEntry(JarInputStream jarStream, String path) throws IOException {
        for (JarEntry jarEntry = null; (jarEntry = jarStream.getNextJarEntry()) != null; ) {
            if (path.equals(jarEntry.getName())) {
                return true;
            }
        }
        return false;
    }

    static JarInputStream getJarInputStream(URL url) throws IOException {
        boolean isEmbedded = url.toExternalForm().contains(".jar!/");
        InputStream stream = getInputStream(url);
        if ((!isEmbedded) && ((stream instanceof JarInputStream))) {
            return (JarInputStream) stream;
        }
        return new JarInputStream(stream);
    }
}