package com.newrelic.agent.util;

import com.newrelic.agent.Agent;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.util.asm.Utils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class JarUtils
{
  public static File createJarFile(String prefix, Map<String, byte[]> classes)
    throws IOException
  {
    return createJarFile(prefix, classes, null);
  }

  public static File createJarFile(String prefix, Map<String, byte[]> classes, Manifest manifest)
    throws IOException
  {
    File file = File.createTempFile(prefix, ".jar");
    file.deleteOnExit();

    if (manifest == null) {
      manifest = new Manifest();
    }
    JarOutputStream outStream = new JarOutputStream(new FileOutputStream(file), manifest);

    writeFilesToJarStream(classes, file, outStream);

    return file;
  }

  private static void writeFilesToJarStream(Map<String, byte[]> classes, File file, JarOutputStream outStream)
    throws IOException
  {
    Map resources = Maps.newHashMap();
    for (Entry entry : classes.entrySet())
      resources.put(Utils.getClassResourceName((String)entry.getKey()), entry.getValue());
    try
    {
      addJarEntries(outStream, resources);
    } finally {
      outStream.close();
    }

    Agent.LOG.finer("Created " + file.getAbsolutePath());
  }

  public static void addJarEntries(JarOutputStream jarStream, Map<String, byte[]> files) throws IOException {
    for (Entry entry : files.entrySet()) {
      JarEntry jarEntry = new JarEntry((String)entry.getKey());

      jarStream.putNextEntry(jarEntry);
      jarStream.write((byte[])entry.getValue());
      jarStream.closeEntry();
    }
  }
}