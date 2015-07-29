package com.newrelic.agent.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureClassLoader;

public class CleverClassLoader extends SecureClassLoader
{
  public CleverClassLoader(ClassLoader parent)
  {
    super(parent);
  }

  public Class loadClassSpecial(String name) throws ClassNotFoundException, IOException
  {
    String fileName = name.replace('.', '/');
    fileName = fileName + ".class";

    InputStream inStream = ClassLoader.getSystemClassLoader().getResourceAsStream(fileName);
    if (inStream == null)
      throw new ClassNotFoundException("Unable to find class " + name);
    try
    {
      ByteArrayOutputStream oStream = new ByteArrayOutputStream();
      Streams.copy(inStream, oStream);
      return loadClass(name, oStream.toByteArray());
    } finally {
      inStream.close();
    }
  }

  public Class<?> loadClass(String name) throws ClassNotFoundException
  {
    if (name.startsWith("com.newrelic")) {
      try {
        return ClassLoader.getSystemClassLoader().loadClass(name);
      } catch (NoClassDefFoundError e) {
        try {
          return loadClassSpecial(name);
        } catch (IOException e1) {
          throw e;
        }
      }
    }
    return super.loadClass(name);
  }

  protected Class loadClass(String name, byte[] bytes)
  {
    return defineClass(name, bytes, 0, bytes.length);
  }
}