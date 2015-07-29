package com.newrelic.bootstrap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

import com.newrelic.deps.com.google.common.cache.CacheBuilder;
import com.newrelic.deps.com.google.common.cache.CacheLoader;
import com.newrelic.deps.com.google.common.cache.LoadingCache;

public class EmbeddedJarFilesImpl implements EmbeddedJarFiles {
  private static final String[] INTERNAL_JAR_FILE_NAMES =
          new String[] {"agent-bridge-1.0", "agent-api-1.0", "weaver-api-1.0"};
  public static final EmbeddedJarFiles INSTANCE = new EmbeddedJarFilesImpl();
  private final LoadingCache<String, File> embeddedAgentJarFiles =
          CacheBuilder.newBuilder().build(new CacheLoader<String, File>() {
            public File load(String jarNameWithoutExtension) throws IOException {
              InputStream jarStream =
                      ClassLoader.getSystemClassLoader().getResourceAsStream(jarNameWithoutExtension + ".jar");

              if (jarStream == null) {
                throw new FileNotFoundException(jarNameWithoutExtension + ".jar");
              }

              File file = File.createTempFile(jarNameWithoutExtension, ".jar");
              file.deleteOnExit();

              OutputStream out = new FileOutputStream(file);
              try {
                BootstrapLoader.copy(jarStream, out, 8096, true);

                return file;
              } finally {
                out.close();
              }
            }
          });
  private final String[] jarFileNames;

  public EmbeddedJarFilesImpl() {
    this(INTERNAL_JAR_FILE_NAMES);
  }

  public EmbeddedJarFilesImpl(String[] jarFileNames) {
    this.jarFileNames = jarFileNames;
  }

  public File getJarFileInAgent(String jarNameWithoutExtension) throws IOException {
    try {
      return embeddedAgentJarFiles.get(jarNameWithoutExtension);
    } catch (ExecutionException e) {
      try {
        throw e.getCause();
      } catch (IOException ex) {
        throw ex;
      } catch (Throwable ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  public String[] getEmbeddedAgentJarFileNames() {
    return jarFileNames;
  }
}