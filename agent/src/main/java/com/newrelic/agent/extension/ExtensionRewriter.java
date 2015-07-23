//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.extension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.RemappingClassAdapter;

import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.util.Streams;

public class ExtensionRewriter {
    static final DependencyRemapper REMAPPER = new DependencyRemapper(ImmutableSet.of("com/newrelic/agent/deps/org"
                                                                                              + "/objectweb/asm/",
                                                                                             "com/newrelic/agent/deps/com/google/",
                                                                                             "com/newrelic/agent/deps/org/apache/commons/"));

    private ExtensionRewriter() {
    }

    public static byte[] rewrite(JarFile jar) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JarOutputStream jarOut = new JarOutputStream(out);
        boolean modified = false;

        try {
            Enumeration e = jar.entries();

            while (e.hasMoreElements()) {
                JarEntry entry = (JarEntry) e.nextElement();
                JarEntry newEntry = new JarEntry(entry.getName());
                Object inputStream = jar.getInputStream(entry);

                try {
                    if (entry.getName().endsWith(".class")) {
                        ClassReader cr = new ClassReader((InputStream) inputStream);
                        ClassWriter writer = new ClassWriter(2);
                        RemappingClassAdapter cv = new RemappingClassAdapter(writer, REMAPPER);
                        cr.accept(cv, 4);
                        if (!REMAPPER.getRemappings().isEmpty()) {
                            modified = true;
                        }

                        ((InputStream) inputStream).close();
                        inputStream = new ByteArrayInputStream(writer.toByteArray());
                    }

                    jarOut.putNextEntry(newEntry);
                    Streams.copy((InputStream) inputStream, jarOut, ((InputStream) inputStream).available());
                } finally {
                    jarOut.closeEntry();
                    ((InputStream) inputStream).close();
                }
            }
        } finally {
            jarOut.close();
            jar.close();
            out.close();
        }

        return modified ? out.toByteArray() : null;
    }
}
