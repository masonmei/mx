package com.newrelic.agent.instrumentation;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.ClassWriter;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMapper;
import com.newrelic.agent.util.Annotations;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

public class InterfaceImplementationClassTransformer extends AbstractImplementationClassTransformer {
  private final Map<Method, java.lang.reflect.Method> methods;
  private final boolean genericInterfaceSupportEnabled;

  public InterfaceImplementationClassTransformer(ClassTransformer classTransformer, boolean enabled,
                                                 Class interfaceToImplement) {
    super(classTransformer, enabled, interfaceToImplement);
    boolean genericInterfaceSupportEnabled = true;

    Map methods2 = Collections.emptyMap();
    InterfaceMapper mapper = (InterfaceMapper) interfaceToImplement.getAnnotation(InterfaceMapper.class);

    Class visitorClass = mapper.classVisitor();
    if (visitorClass == Object.class) {
      visitorClass = InterfaceImplementationClassVisitor.class;
    }
    if (visitorClass == InterfaceImplementationClassVisitor.class) {
      genericInterfaceSupportEnabled = false;
      methods2 = MethodMappersAdapter.getMethodMappers(interfaceToImplement);
    }
    methods = Collections.unmodifiableMap(methods2);

    this.genericInterfaceSupportEnabled = ((genericInterfaceSupportEnabled) && (mapper.className().length == 0));
  }

  public static StartableClassFileTransformer[] getClassTransformers(ClassTransformer classTransformer) {
    Collection<Class<?>> interfaces = Annotations.getAnnotationClassesFromManifest(InterfaceMapper.class,
                                                                                          "com/newrelic/agent/instrumentation/pointcuts");

    List transformers = new ArrayList(interfaces.size());

    for (Class interfaceClass : interfaces) {
      transformers.add(new InterfaceImplementationClassTransformer(classTransformer, true, interfaceClass));
    }

    return (StartableClassFileTransformer[]) transformers.toArray(new StartableClassFileTransformer[0]);
  }

  protected boolean isGenericInterfaceSupportEnabled() {
    return genericInterfaceSupportEnabled;
  }

  protected ClassVisitor createClassVisitor(ClassReader cr, ClassWriter cw, String className, ClassLoader loader) {
    InterfaceMapper mapper = (InterfaceMapper) interfaceToImplement.getAnnotation(InterfaceMapper.class);
    Set<Method> methods2 = new HashSet(methods.keySet());

    Class classVisitorClass = mapper.classVisitor();
    if (classVisitorClass == Object.class) {
      classVisitorClass = InterfaceImplementationClassVisitor.class;
    }
    if (InterfaceImplementationClassVisitor.class == classVisitorClass) {
      ClassVisitor classVisitor = new AddInterfaceAdapter(cw, className, interfaceToImplement);
      classVisitor = RequireMethodsAdapter.getRequireMethodsAdaptor(classVisitor, methods2, className,
                                                                           interfaceToImplement.getName(),
                                                                           loader);

      classVisitor =
              MethodMappersAdapter.getMethodMappersAdapter(classVisitor, methods, originalInterface, className);

      return classVisitor;
    }
    if (ClassVisitor.class.isAssignableFrom(mapper.classVisitor())) {
      try {
        Constructor constructor =
                mapper.classVisitor().getConstructor(new Class[] {ClassVisitor.class, String.class});
        return (ClassVisitor) constructor.newInstance(new Object[] {cw, className});
      } catch (Throwable e) {
        Agent.LOG.log(Level.FINEST, "while creating ClassVisitor for InterfaceMapper transformation", e);
      }
    }
    Agent.LOG.log(Level.FINEST, "Unable to create ClassVisitor (type {0}) for {1} with loader {2}",
                         new Object[] {classVisitorClass, className, loader});

    return cw;
  }

  public class InterfaceImplementationClassVisitor extends ClassVisitor {
    public InterfaceImplementationClassVisitor(int api) {
      super(api);
    }
  }
}