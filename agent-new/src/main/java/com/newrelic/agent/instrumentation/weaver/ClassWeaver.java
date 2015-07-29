package com.newrelic.agent.instrumentation.weaver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import com.newrelic.deps.org.objectweb.asm.AnnotationVisitor;
import com.newrelic.deps.org.objectweb.asm.Attribute;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.FieldVisitor;
import com.newrelic.deps.org.objectweb.asm.Label;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.GeneratorAdapter;
import com.newrelic.deps.org.objectweb.asm.commons.JSRInlinerAdapter;
import com.newrelic.deps.org.objectweb.asm.commons.Method;
import com.newrelic.deps.org.objectweb.asm.commons.Remapper;
import com.newrelic.deps.org.objectweb.asm.tree.FieldNode;
import com.newrelic.deps.org.objectweb.asm.tree.InnerClassNode;
import com.newrelic.deps.org.objectweb.asm.tree.MethodNode;

import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.agent.util.asm.ClassStructure;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.org.objectweb.asm.commons.MethodCallInlinerAdapter;

class ClassWeaver extends ClassVisitor {
  private static final Remapper NO_OP_REMAPPER = new Remapper() {
  };
  private static final String MAGIC_KEY_FOR_CONSTRUCTOR_INLINE = "____INLINE_ME____";
  private final Set<Method> originalMethods = new HashSet();
  private final String className;
  private final Map<Method, MergeMethodVisitor> methods;
  private final Map<String, FieldNode> newFields;
  private final Map<String, FieldNode> existingFields = Maps.newHashMap();
  private final Verifier verifier;
  private final MixinClassVisitor mixinClassVisitor;
  private final Map<Method, MergeMethodVisitor> newMethods;
  private final InstrumentationContext context;
  private final InstrumentationPackage instrumentationPackage;
  private boolean firstField = true;
  private int version;

  public ClassWeaver(ClassVisitor cv, MixinClassVisitor mixinClassVisitor, String className, Verifier verifier,
                     ClassStructure originalClassStructure, InstrumentationContext context,
                     InstrumentationPackage instrumentationPackage, OptimizedClassMatcher.Match match) {
    super(Agent.ASM_LEVEL, cv);

    this.verifier = verifier;
    this.className = className;
    this.mixinClassVisitor = mixinClassVisitor;
    methods = Maps.newHashMap(mixinClassVisitor.getMethods());

    Map tracedMethods = Maps.newHashMap(mixinClassVisitor.getWeaveClassInfo().getTracedMethods());
    for (Entry<Method, Method> entry : context.getBridgeMethods().entrySet()) {

      MergeMethodVisitor mv = methods.remove(entry.getKey());
      if (mv != null) {
        methods.put(entry.getValue(), mv);
        TraceDetails traceDetails = (TraceDetails) tracedMethods.remove(entry.getKey());
        if (traceDetails != null) {
          tracedMethods.put(entry.getValue(), traceDetails);
        }
      }
    }

    this.context = context;
    this.instrumentationPackage = instrumentationPackage;

    newFields = mixinClassVisitor.getWeaveClassInfo().getNewFields();

    newMethods = Maps.newHashMap(mixinClassVisitor.getMethods());
    for (Method method : originalClassStructure.getMethods()) {
      newMethods.remove(method);
    }

    Set toRemove = Sets.newHashSet();
    for (Method newMethod : newMethods.keySet()) {
      TraceDetails traceDetails = (TraceDetails) tracedMethods.get(newMethod);
      if (traceDetails != null) {
        Level level = MatchType.ExactClass.equals(mixinClassVisitor.getMatchType()) ? Level.FINE : Level.FINER;
        instrumentationPackage.getLogger().log(level, newMethod + " is marked with a Trace annotation, but it "
                                                              + "does not exist on " + className + ".");
      }

    }

    newMethods.keySet().removeAll(toRemove);

    context.addTracedMethods(tracedMethods);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    this.version = version;
    super.visit(version, access, name, signature, superName, interfaces);
  }

  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    if (firstField) {
      firstField = false;
      for (InnerClassNode innerClass : mixinClassVisitor.getInnerClasses()) {
        if ((mixinClassVisitor.isAbstractMatch()) &&
                    (!instrumentationPackage.getWeaveClasses().containsKey(innerClass.name))
                    && (instrumentationPackage.getClassBytes().keySet().contains(innerClass.name))) {
          throw new IllegalInstructionException("Inner classes are not currently supported for abstract "
                                                        + "merged classes.  " + className + " : "
                                                        + innerClass.name);
        }

        if ((!instrumentationPackage.isWeaved(innerClass.name)) && (instrumentationPackage
                                                                            .matches(innerClass.name))) {
          visitInnerClass(innerClass.name, innerClass.outerName, innerClass.innerName, innerClass.access);
        }
      }
    }
    FieldNode node = new FieldNode(access, name, desc, signature, value);
    existingFields.put(node.name, node);
    return super.visitField(access, name, desc, signature, value);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    Method method = new Method(name, desc);
    originalMethods.add(method);

    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ((access & 0x400) != 0) {
      methods.remove(method);
      return mv;
    }

    MergeMethodVisitor replayMethodVisitor = (MergeMethodVisitor) methods.get(method);
    if ("<init>".equals(name)) {
      if ((!"()V".equals(desc)) && (replayMethodVisitor == null)) {
        replayMethodVisitor = (MergeMethodVisitor) methods.get(new Method(name, "()V"));
      }

      if (replayMethodVisitor != null) {
        mv = new ConstructorMerger(access, mv, name, desc, signature, exceptions, replayMethodVisitor);
      }

      mv = mixinClassVisitor.getWeaveClassInfo().getConstructorMethodVisitor(mv, className, access, name, desc);

      replayMethodVisitor = null;
    }

    if (null != replayMethodVisitor) {
      methods.remove(method);

      if (replayMethodVisitor.isNewMethod()) {
        throw new IllegalInstructionException("Weaved method " + className + '.' + name + desc
                                                      + " does not call the original method implementation");
      }

      instrumentationPackage.getLogger().fine("Injecting code into " + className + '.' + method);

      mv = new MethodMerger(access, mv, replayMethodVisitor, name, desc, signature, exceptions);

      context.addWeavedMethod(method, instrumentationPackage.getImplementationTitle());
    }

    mv = new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions);

    return mv;
  }

  private class MethodMerger extends MethodNode {
    private final MergeMethodVisitor codeToInject;
    private final MethodVisitor writer;
    private final String name;
    private final String desc;
    private final int access;
    private final Method method;

    public MethodMerger(int access, MethodVisitor mv, MergeMethodVisitor codeToInject, String name, String desc,
                        String signature, String[] exceptions) {
      super(access, name, desc, signature, exceptions);
      writer = mv;
      this.codeToInject = codeToInject;
      method = new Method(name, desc);
      this.name = name;
      this.desc = desc;
      this.access = access;
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      return writer.visitAnnotation(desc, visible);
    }

    public AnnotationVisitor visitAnnotationDefault() {
      return writer.visitAnnotationDefault();
    }

    public void visitAttribute(Attribute attr) {
      writer.visitAttribute(attr);
    }

    public void visitEnd() {
      final HashMap<Method, MethodNode> methodsToInline = Maps.newHashMap(mixinClassVisitor.getMethodsToInline());

      instructions.resetLabels();

      codeToInject.instructions.resetLabels();

      for (MethodNode newCode : methodsToInline.values()) {
        newCode.instructions.resetLabels();
      }

      MethodVisitor mv = new MethodCallInlinerAdapter(className, access, name, desc, writer, false) {
        protected InlinedMethod mustInline(String owner, String name, String desc) {
          if ((owner.equals(codeToInject.getClassName())) && (new Method(name, desc)
                                                                      .equals(codeToInject.getMethod()))) {
            instrumentationPackage.getLogger().finer("Inline original implementation of " + name + desc);
            return new InlinedMethod(MethodMerger.this, ClassWeaver.NO_OP_REMAPPER);
          }
          return null;
        }
      };
      mv = mixinClassVisitor.getWeaveClassInfo().getMethodVisitor(className, mv, access, method);

      mv = new MethodCallInlinerAdapter(className, access, name, desc, mv, false) {
        protected InlinedMethod mustInline(String owner, String name, String desc) {
          MethodNode methodToInline = methodsToInline.get(new Method(name, desc));
          if ((owner.equals(codeToInject.getClassName())) && (methodToInline != null)) {
            instrumentationPackage.getLogger().finer("Inlining " + name + desc);
            return new InlinedMethod(methodToInline, ClassWeaver.NO_OP_REMAPPER);
          }
          return null;
        }
      };
      if (version < 49) {
        mv = new FixLoadClassMethodAdapter(access, method, mv);
      }

      codeToInject.accept(mv);
    }

    public void visitMaxs(int maxStack, int maxLocals) {
      writer.visitMaxs(0, 0);
    }
  }

  private class ConstructorMerger extends MethodNode {
    private final MethodVisitor writer;
    private final MethodNode newCode;
    private final Method method;

    public ConstructorMerger(int access, MethodVisitor mv, String name, String desc, String signature,
                             String[] exceptions, MethodNode additionalCode) {
      super(access, name, desc, signature, exceptions);

      method = new Method(name, desc);

      newCode = new MethodNode(Agent.ASM_LEVEL, additionalCode.access, additionalCode.name, additionalCode.desc,
                                      additionalCode.signature,
                                      (String[]) additionalCode.exceptions.toArray(new String[0]));

      MethodVisitor codeVisitor = newCode;

      codeVisitor = mixinClassVisitor.getWeaveClassInfo()
                            .getMethodVisitor(className, codeVisitor, access, new Method(name, desc));

      additionalCode.accept(codeVisitor);

      writer = mv;
    }

    public void visitInsn(int opcode) {
      if (177 == opcode) {
        GeneratorAdapter adapter = new GeneratorAdapter(access, method, this);
        Label start = adapter.newLabel();
        Label end = adapter.newLabel();
        Label handler = adapter.newLabel();
        adapter.visitLabel(start);

        adapter.visitVarInsn(25, 0);

        adapter.loadArgs();
        adapter.visitMethodInsn(182, MAGIC_KEY_FOR_CONSTRUCTOR_INLINE, newCode.name, method.getDescriptor(), false);

        adapter.goTo(end);

        adapter.visitLabel(handler);

        if (Agent.isDebugEnabled()) {
          adapter.invokeVirtual(Type.getType(Throwable.class), new Method("printStackTrace", "()V"));
        } else {
          adapter.pop();
        }

        adapter.visitLabel(end);
        adapter.visitTryCatchBlock(start, end, handler, Type.getType(Throwable.class).getInternalName());
      }
      super.visitInsn(opcode);
    }

    public void visitEnd() {
      instructions.resetLabels();

      MethodVisitor mv = writer;

      mv = new MethodCallInlinerAdapter(className, access, name, desc, mv, false) {
        protected InlinedMethod mustInline(String owner, String name, String desc) {
          if (MAGIC_KEY_FOR_CONSTRUCTOR_INLINE.equals(owner)) {
            instrumentationPackage.getLogger().finer("Inline constructor " + name);
            return new InlinedMethod(newCode, ClassWeaver.NO_OP_REMAPPER);
          }
          return null;
        }
      };
      accept(mv);
    }
  }
}