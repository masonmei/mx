//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.ClassWriter;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;
import com.newrelic.agent.util.Annotations;
import com.newrelic.agent.util.Strings;

public class InterfaceMixinClassTransformer extends AbstractClassTransformer {
  private final Map<String, List<Class<?>>> interfaceVisitors = new HashMap();

  public InterfaceMixinClassTransformer(int classreaderFlags) {
    super(classreaderFlags);
  }

  protected void start() {
    this.addInterfaceMixins();
  }

  private void addInterfaceMixins() {
    Collection classes = Annotations.getAnnotationClassesFromManifest(InterfaceMixin.class,
                                                                             "com/newrelic/agent/instrumentation/pointcuts");
    Iterator i$ = classes.iterator();

    while (i$.hasNext()) {
      Class clazz = (Class) i$.next();
      this.addInterfaceMixin(clazz);
    }

  }

  protected void addInterfaceMixin(Class<?> clazz) {
    if (clazz != null) {
      InterfaceMixin mixin = clazz.getAnnotation(InterfaceMixin.class);
      if (mixin == null) {
        Agent.LOG.log(Level.FINER, "InterfaceMixin access failed: " + clazz.getName());
      } else {
        String[] arr$ = mixin.originalClassName();
        int len$ = arr$.length;

        for (int i$ = 0; i$ < len$; ++i$) {
          String className = arr$[i$];
          String key = Strings.fixInternalClassName(className);
          List<Class<?>> value = (List) this.interfaceVisitors.get(key);
          if (value == null) {
            value = new ArrayList<Class<?>>(1);
          }

          value.add(clazz);
          this.interfaceVisitors.put(key, value);
        }

      }
    }
  }

  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                          ProtectionDomain protectionDomain, byte[] classfileBuffer)
          throws IllegalClassFormatException {
    try {
      if (!this.matches(loader, className, classBeingRedefined, protectionDomain, classfileBuffer)) {
        return null;
      } else if (!this.isAbleToResolveAgent(loader, className)) {
        return null;
      } else {
        List t = (List) this.interfaceVisitors.get(className);
        if (t != null && t.size() != 0) {
          byte[] msg1 = InstrumentationUtils.generateClassBytesWithSerialVersionUID(classfileBuffer,
                                                                                           this.getClassReaderFlags(),
                                                                                           loader);
          return this.transform(msg1, t, loader, className);
        } else {
          return null;
        }
      }
    } catch (Throwable var8) {
      String msg = MessageFormat.format("Instrumentation error for {0}: {1}", new Object[] {className, var8});
      Agent.LOG.log(Level.FINER, msg, var8);
      return null;
    }
  }

  private byte[] transform(byte[] classBytesWithUID, List<Class<?>> clazzes, ClassLoader loader, String className)
          throws Exception {
    byte[] classBytes = classBytesWithUID;
    byte[] oldClassBytes = classBytesWithUID;
    Iterator msg = clazzes.iterator();

    while (msg.hasNext()) {
      Class clazz = (Class) msg.next();

      try {
        classBytes = this.transform(classBytes, clazz, loader, className);
        oldClassBytes = classBytes;
      } catch (StopProcessingException var11) {
        String msg1 = MessageFormat.format("Failed to append {0} to {1}: {2}",
                                                  new Object[] {clazz.getName(), className, var11});
        Agent.LOG.fine(msg1);
        classBytes = oldClassBytes;
      }
    }

    String msg2 = MessageFormat.format("Instrumenting {0}", new Object[] {className});
    Agent.LOG.finer(msg2);
    return classBytes;
  }

  private byte[] transform(byte[] classBytes, Class<?> clazz, ClassLoader loader, String className) throws Exception {
    ClassReader cr = new ClassReader(classBytes);
    ClassWriter cw = InstrumentationUtils.getClassWriter(cr, loader);
    ClassVisitor classVisitor = this.getClassVisitor((ClassVisitor) cw, (String) className, (Class) clazz, loader);
    cr.accept(classVisitor, this.getClassReaderFlags());
    Agent.LOG.log(Level.FINEST, "InterfaceMixingClassTransformer.transform(bytes, clazz, {0}, {1})",
                         new Object[] {loader, className});
    return cw.toByteArray();
  }

  private ClassVisitor getClassVisitor(ClassVisitor classVisitor, String className, Class<?> clazz,
                                       ClassLoader loader) {
    AddInterfaceAdapter adapter = new AddInterfaceAdapter(classVisitor, className, clazz);
    RequireMethodsAdapter adapter1 =
            RequireMethodsAdapter.getRequireMethodsAdaptor(adapter, className, clazz, loader);
    FieldAccessorGeneratingClassAdapter adapter2 =
            new FieldAccessorGeneratingClassAdapter(adapter1, className, clazz);
    return adapter2;
  }

  protected boolean isRetransformSupported() {
    return false;
  }

  protected boolean matches(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
    return this.interfaceVisitors.containsKey(className);
  }

  protected ClassVisitor getClassVisitor(ClassReader cr, ClassWriter cw, String className, ClassLoader loader) {
    return null;
  }
}
