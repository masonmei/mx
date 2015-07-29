package com.newrelic.agent.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.reflections.Reflections;
import com.newrelic.deps.org.reflections.serializers.JsonSerializer;
import com.newrelic.deps.org.reflections.util.ConfigurationBuilder;

import com.newrelic.deps.com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentJarHelper;
import com.newrelic.agent.config.JarResource;
import com.newrelic.agent.util.asm.ClassStructure;

public class Annotations {
  private static Reflections loaded;

  public static Collection<Class<?>> getAnnotationClasses(Class<?> annotationClass, String packageSearchPath) {
    String pointcutAnnotation = 'L' + annotationClass.getName().replace('.', '/') + ';';
    if (!packageSearchPath.endsWith("/")) {
      packageSearchPath = packageSearchPath + "/";
    }
    Pattern pattern = Pattern.compile(packageSearchPath + "(.*).class");
    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    JarResource agentJarFile = AgentJarHelper.getAgentJarResource();
    try {
      Collection<String> fileNames = AgentJarHelper.findAgentJarFileNames(pattern);
      Collection<Class<?>> classes = new ArrayList<Class<?>>(fileNames.size());
      for (String fileName : fileNames) {
        int size = (int) agentJarFile.getSize(fileName);
        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        try {
          Streams.copy(agentJarFile.getInputStream(fileName), out, size, true);
          ClassReader cr = new ClassReader(out.toByteArray());
          ClassStructure structure = ClassStructure.getClassStructure(cr, 4);
          Collection annotations = structure.getClassAnnotations().keySet();
          if (annotations.contains(pointcutAnnotation)) {
            String className = fileName.replace('/', '.');
            int index = className.indexOf(".class");
            if (index > 0) {
              className = className.substring(0, index);
            }
            Class clazz = classLoader.loadClass(className);
            classes.add(clazz);
          }
        } catch (Exception e) {
          Agent.LOG.log(Level.FINEST, e, e.toString(), new Object[0]);
        }
      }
      return classes;
    } finally {
      try {
        agentJarFile.close();
      } catch (IOException e) {
        Agent.LOG.log(Level.FINEST, e, e.toString(), new Object[0]);
      }
    }
  }

  public static Collection<Class<?>> getAnnotationClassesFromManifest(Class<? extends Annotation> annotationClass,
                                                                      String packageSearchPath) {
    if (loaded == null) {
      JarResource agentJarFile = AgentJarHelper.getAgentJarResource();
      try {
        Reflections loader = new Reflections(new ConfigurationBuilder().setSerializer(new JsonSerializer()));
        loaded = loader.collect(agentJarFile.getInputStream("newrelic-manifest.json"));
      } catch (Exception e) {
        return Collections.emptySet();
      } finally {
        try {
          agentJarFile.close();
        } catch (IOException e) {
          Agent.LOG.log(Level.FINEST, e, e.toString(), new Object[0]);
        }
      }
    }

    Set<Class<?>> annotationClasses = loaded.getTypesAnnotatedWith(annotationClass);
    packageSearchPath = packageSearchPath.replaceAll("/", ".");
    Set filteredAnnotationClasses = Sets.newHashSetWithExpectedSize(annotationClasses.size());
    for (Class annotationClassValue : annotationClasses) {
      if (annotationClassValue.getName().startsWith(packageSearchPath)) {
        filteredAnnotationClasses.add(annotationClassValue);
      }
    }
    return filteredAnnotationClasses;
  }
}