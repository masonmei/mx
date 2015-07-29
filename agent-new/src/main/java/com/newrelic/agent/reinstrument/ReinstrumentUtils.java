//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.reinstrument;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.extension.beans.Extension;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method;
import com.newrelic.agent.extension.beans.MethodParameters;
import com.newrelic.agent.extension.util.ExtensionConversionUtility;
import com.newrelic.agent.extension.util.MethodMapper;
import com.newrelic.agent.extension.util.MethodMatcherUtility;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.asm.ClassStructure;

public class ReinstrumentUtils {
  public ReinstrumentUtils() {
  }

  public static void checkClassExistsAndRetransformClasses(ReinstrumentResult result,
                                                           List<ExtensionClassAndMethodMatcher> pcs, Extension ext,
                                                           Set<Class<?>> classesToRetransform) {
    if (!pcs.isEmpty()) {
      HashSet loaders = new HashSet();
      HashMap toRetransform = Maps.newHashMap();
      getLoadedClassData(pcs, loaders, toRetransform);
      checkInputClasses(result, loaders, ext, toRetransform);
    }

    retransform(result, classesToRetransform);
  }

  private static void getLoadedClassData(List<ExtensionClassAndMethodMatcher> pcs, Set<ClassLoader> loaders,
                                         Map<String, Class<?>> toRetransform) {
    Class[] allLoadedClasses = ServiceFactory.getAgent().getInstrumentation().getAllLoadedClasses();
    if (allLoadedClasses != null) {
      Class[] arr$ = allLoadedClasses;
      int len$ = allLoadedClasses.length;

      for (int i$ = 0; i$ < len$; ++i$) {
        Class current = arr$[i$];

        try {
          if (current != null) {
            if (current.getClassLoader() != null) {
              loaders.add(current.getClassLoader());
            }

            if (shouldTransform(current, pcs)) {
              toRetransform.put(current.getName(), current);
            }
          }
        } catch (Exception var9) {
          Agent.LOG
                  .log(Level.FINE, "An unexpected exception occured examining a class for retransformation.");
          if (Agent.LOG.isFinestEnabled()) {
            Agent.LOG.log(Level.FINEST, "An exception occured examining a class for retransformation.",
                                 var9);
          }
        }
      }
    }

  }

  public static void retransform(ReinstrumentResult result, Set<Class<?>> classesToRetransform) {
    try {
      if (!classesToRetransform.isEmpty()) {
        ServiceFactory.getAgent().getInstrumentation().retransformClasses((Class[]) classesToRetransform
                                                                                            .toArray(new Class[classesToRetransform
                                                                                                                       .size()]));
        result.setRetranformedInitializedClasses(getClassNames(classesToRetransform));
      }
    } catch (Exception var3) {
      handleError(result, MessageFormat.format("Attempt to retransform classes failed. Message: {0}",
                                                      new Object[] {var3.getMessage()}), var3);
    }

  }

  private static Set<String> getClassNames(Set<Class<?>> classes) {
    HashSet names = Sets.newHashSet();
    Iterator i$ = classes.iterator();

    while (i$.hasNext()) {
      Class clazz = (Class) i$.next();
      names.add(clazz.getName());
    }

    return names;
  }

  private static void performRetransformations(ReinstrumentResult result, Map<String, Class<?>> toRetransform) {
    try {
      int e = toRetransform.size();
      if (e > 0) {
        ServiceFactory.getAgent().getInstrumentation()
                .retransformClasses((Class[]) toRetransform.values().toArray(new Class[e]));
        result.setRetranformedInitializedClasses(toRetransform.keySet());
      }
    } catch (Exception var3) {
      handleError(result, MessageFormat.format("Attempt to retransform classes failed. Message: {0}",
                                                      new Object[] {var3.getMessage()}), var3);
    }

  }

  private static void handleError(ReinstrumentResult result, String msg, Exception e) {
    result.addErrorMessage(msg);
    Agent.LOG.log(Level.INFO, msg);
    if (Agent.LOG.isFinestEnabled()) {
      Agent.LOG.log(Level.FINEST, msg, e);
    }

  }

  protected static void handleErrorPartialInstrumentation(ReinstrumentResult result, List<Exception> msgs,
                                                          String pXml) {
    if (msgs != null && msgs.size() > 0) {
      Iterator i$ = msgs.iterator();

      while (i$.hasNext()) {
        Exception msg = (Exception) i$.next();
        result.addErrorMessage(msg.getMessage());
        Agent.LOG.log(Level.INFO, msg.getMessage());
      }

      if (Agent.LOG.isFinerEnabled()) {
        Agent.LOG.log(Level.FINER, MessageFormat.format("Errors occured when processing this xml: {0}",
                                                               new Object[] {pXml}));
      }
    }

  }

  protected static void handleErrorPartialInstrumentation(ReinstrumentResult result, String msg) {
    Agent.LOG.log(Level.INFO, msg);
    result.addErrorMessage(msg);
    if (Agent.LOG.isFinerEnabled()) {
      Agent.LOG.log(Level.FINER, MessageFormat.format("Errors occured when processing this xml: {0}",
                                                             new Object[] {msg}));
    }

  }

  protected static void checkInputClasses(ReinstrumentResult result, Set<ClassLoader> loaders, Extension ext,
                                          Map<String, Class<?>> toRetransform) {
    if (ext.getInstrumentation() != null) {
      List pcs = ext.getInstrumentation().getPointcut();
      Iterator i$ = pcs.iterator();

      while (i$.hasNext()) {
        Pointcut pointcut = (Pointcut) i$.next();
        if (pointcut.getMethodAnnotation() == null) {
          checkForClassAndMethods(result, loaders, ExtensionConversionUtility.getClassName(pointcut),
                                         toRetransform, pointcut);
        }
      }
    }

  }

  private static void checkForClassAndMethods(ReinstrumentResult result, Set<ClassLoader> loaders, String className,
                                              Map<String, Class<?>> toRetransform, Pointcut pc) {
    if (className != null) {
      Class current = (Class) toRetransform.get(className);
      if (current != null) {
        checkMethodsInClass(result, ClassStructure.getClassStructure(current), pc);
      } else {
        Iterator i$ = loaders.iterator();

        while (true) {
          URL resource;
          do {
            ClassLoader loader;
            do {
              if (!i$.hasNext()) {
                handleErrorPartialInstrumentation(result, MessageFormat.format("The class {0} does not "
                                                                                       + "match a "
                                                                                       + "loaded "
                                                                                       + "class in "
                                                                                       + "the JVM"
                                                                                       + ". Either the "
                                                                                       + "class has "
                                                                                       + "not "
                                                                                       + "been loaded"
                                                                                       + " yet "
                                                                                       + "or it does "
                                                                                       + "not "
                                                                                       + "exist.",
                                                                                      new Object[]
                                                                                              {className}));
                return;
              }

              loader = (ClassLoader) i$.next();
            } while (loader == null);

            resource = loader.getResource(className.replace(".", "/") + ".class");
          } while (resource == null);

          try {
            checkMethodsInClass(result, ClassStructure.getClassStructure(resource), pc);
            return;
          } catch (IOException var10) {
            Agent.LOG.log(Level.FINER, "Error validating class " + className, var10);
          }
        }
      }
    }

  }

  private static void checkMethodsInClass(ReinstrumentResult result, ClassStructure classStructure, Pointcut pc) {
    List desiredMethods = pc.getMethod();
    if (desiredMethods != null) {
      Set actualMethods = classStructure.getMethods();
      Iterator i$ = desiredMethods.iterator();

      while (i$.hasNext()) {
        Method m = (Method) i$.next();
        if (!foundMethod(m, actualMethods)) {
          handleErrorPartialInstrumentation(result, MessageFormat
                                                            .format("The method {0} with parameter type {1}"
                                                                            + " on class {2} is not present"
                                                                            + " and therefore will never "
                                                                            + "match anything.", m.getName(),
                                                                           MethodParameters
                                                                                   .getDescriptor(m.getParameters()),
                                                                           ExtensionConversionUtility
                                                                                   .getClassName(pc)));
        }
      }
    }

  }

  private static boolean foundMethod(Method method, Set<com.newrelic.deps.org.objectweb.asm.commons.Method> actualMethods) {
    try {
      MethodMatcher ex = MethodMatcherUtility.createMethodMatcher("BogusClass", method,
                                                                         Maps.<String, MethodMapper>newHashMap(),
                                                                         "");
      Iterator i$ = actualMethods.iterator();

      while (i$.hasNext()) {
        com.newrelic.deps.org.objectweb.asm.commons.Method m = (com.newrelic.deps.org.objectweb.asm.commons.Method) i$.next();
        if (ex.matches(-1, m.getName(), m.getDescriptor(), MethodMatcher.UNSPECIFIED_ANNOTATIONS)) {
          return true;
        }
      }
    } catch (Exception var5) {
      var5.printStackTrace();
    }

    return false;
  }

  private static boolean shouldTransform(Class<?> clazz, List<ExtensionClassAndMethodMatcher> newPcs) {
    Iterator i$ = newPcs.iterator();

    ExtensionClassAndMethodMatcher pc;
    do {
      if (!i$.hasNext()) {
        return false;
      }

      pc = (ExtensionClassAndMethodMatcher) i$.next();
    } while (!pc.getClassMatcher().isMatch(clazz));

    return true;
  }
}
