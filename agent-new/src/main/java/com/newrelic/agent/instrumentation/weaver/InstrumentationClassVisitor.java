//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.weaver;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import com.newrelic.deps.org.objectweb.asm.AnnotationVisitor;
import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.ClassWriter;
import com.newrelic.deps.org.objectweb.asm.FieldVisitor;
import com.newrelic.deps.org.objectweb.asm.Label;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.AdviceAdapter;
import com.newrelic.deps.org.objectweb.asm.commons.GeneratorAdapter;
import com.newrelic.deps.org.objectweb.asm.commons.JSRInlinerAdapter;
import com.newrelic.deps.org.objectweb.asm.commons.Method;
import com.newrelic.deps.org.objectweb.asm.tree.FieldNode;
import com.newrelic.deps.org.objectweb.asm.tree.InnerClassNode;
import com.newrelic.deps.org.objectweb.asm.tree.MethodNode;

import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ObjectFieldManager;
import com.newrelic.agent.bridge.reflect.ClassReflection;
import com.newrelic.agent.instrumentation.InstrumentationImpl;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.context.CurrentTransactionRewriter;
import com.newrelic.agent.instrumentation.tracing.Annotation;
import com.newrelic.agent.instrumentation.tracing.BridgeUtils;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.CatchAndLog;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.SkipIfPresent;

class InstrumentationClassVisitor extends ClassVisitor implements WeavedClassInfo {
  private static final String OBJECT_FIELDS_FIELD_NAME = "objectFieldManager";
  private static final Method INITIALIZE_FIELDS_METHOD;
  private static final Method GET_FIELD_CONTAINER_METHOD;

  static {
    INITIALIZE_FIELDS_METHOD = new Method("initializeFields", Type.VOID_TYPE,
                                                 new Type[] {Type.getType(String.class), Type.getType(Object.class),
                                                                    Type.getType(Object.class)});
    GET_FIELD_CONTAINER_METHOD = new Method("getFieldContainer", Type.getType(Object.class),
                                                   new Type[] {Type.getType(String.class),
                                                                      Type.getType(Object.class)});
  }

  final Set<InnerClassNode> innerClasses = Sets.newHashSet();
  private final String className;
  private final Map<String, FieldNode> newFields = Maps.newHashMap();
  private final Map<String, FieldNode> existingFields = Maps.newHashMap();
  private final Set<Method> weavedMethods = Sets.newHashSet();
  private final Set<Method> catchAndLogMethods = Sets.newHashSet();
  private final Map<Method, TraceDetails> tracedMethods = Maps.newHashMap();
  private final Map<Method, MethodNode> constructors = Maps.newHashMap();
  private final WeaveMatchTypeAccessor weaveAnnotation = new WeaveMatchTypeAccessor();
  private final InstrumentationPackage instrumentationPackage;
  private final String superName;
  private MethodNode staticConstructor;
  private boolean hasNewInstanceField;
  private boolean skipIfPresent;

  private InstrumentationClassVisitor(InstrumentationPackage instrumentationPackage, String className,
                                      String superName) {
    super(Agent.ASM_LEVEL);
    this.className = className;
    this.superName = superName;
    this.instrumentationPackage = instrumentationPackage;
  }

  static InstrumentationClassVisitor getInstrumentationClass(InstrumentationPackage instrumentationPackage,
                                                             byte[] bytes) {
    ClassReader reader = new ClassReader(bytes);
    InstrumentationClassVisitor cv =
            new InstrumentationClassVisitor(instrumentationPackage, reader.getClassName(), reader.getSuperName());
    reader.accept(cv, 6);
    return cv;
  }

  public static String getFieldContainerClassName(String ownerClass) {
    return "weave/newrelic/" + ownerClass + "$NewRelicFields";
  }

  public static void performSecondPassProcessing(InstrumentationPackage instrumentationPackage,
                                                 Map<String, InstrumentationClassVisitor> instrumentationClasses,
                                                 Map<String, WeavedClassInfo> weaveClasses,
                                                 Map<String, byte[]> classBytes, List<String> newClassLoadOrder) {
    Iterator<Entry<String, InstrumentationClassVisitor>> iterator = instrumentationClasses.entrySet().iterator();

    while (iterator.hasNext()) {
      Entry<String, InstrumentationClassVisitor> entry = iterator.next();
      byte[] bytes = classBytes.get(entry.getKey());
      ClassReader reader = new ClassReader(bytes);
      ClassWriter writer = new ClassWriter(1);
      ClassVisitor cv = entry.getValue()
                                .createSecondPassVisitor(instrumentationPackage, classBytes, newClassLoadOrder,
                                                                weaveClasses, reader, writer);
      reader.accept(cv, 8);
      classBytes.put(entry.getKey(), writer.toByteArray());
    }

  }

  static AdviceAdapter catchAndLogMethodExceptions(final String instrumentationTitle, final String className,
                                                   final int access, final String name, final String desc,
                                                   final MethodVisitor mv) {
    return new AdviceAdapter(Agent.ASM_LEVEL, mv, access, name, desc) {
      Label start = this.newLabel();
      Label end = this.newLabel();
      Label handler = this.newLabel();

      protected void onMethodEnter() {
        super.onMethodEnter();
        this.visitLabel(this.start);
      }

      public void visitMaxs(int maxStack, int maxLocals) {
        super.visitLabel(this.handler);
        final int throwableLocal = this.newLocal(Type.getType(Throwable.class));
        this.storeLocal(throwableLocal);
        Runnable throwableMessage = new Runnable() {
          public void run() {
            loadLocal(throwableLocal);
          }
        };
        BridgeUtils.getLogger(this)
                .logToChild(instrumentationTitle, Level.FINE, "{0}.{1}{2} threw an exception: {3}",
                                   new Object[] {className, name, desc, throwableMessage});
        BridgeUtils.loadLogger(this);
        this.getStatic(Type.getType(Level.class), Level.FINEST.getName(), Type.getType(Level.class));
        this.loadLocal(throwableLocal);
        this.push("Exception stack:");
        this.visitInsn(1);
        ((Logger) BridgeUtils.getLoggerBuilder(this, false).build())
                .log(Level.FINEST, (Exception) null, (String) null, new Object[0]);
        this.visitInsn(177);
        super.visitLabel(this.end);
        super.visitTryCatchBlock(this.start, this.end, this.handler, Type.getInternalName(Throwable.class));
        super.visitMaxs(maxStack, maxLocals);
      }
    };
  }

  static ClassVisitor enforceTracedMethodsAccessingTracedMethod(final ClassVisitor cv, final IAgentLogger logger,
                                                                final String className,
                                                                final Set<Method> tracedMethods) {
    return new ClassVisitor(Agent.ASM_LEVEL, cv) {
      public MethodVisitor visitMethod(int access, String methodName, String methodDesc, String signature,
                                       String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, methodName, methodDesc, signature, exceptions);
        final Method method = new Method(methodName, methodDesc);
        if (!tracedMethods.contains(method)) {
          mv = new MethodVisitor(Agent.ASM_LEVEL, mv) {
            public void visitFieldInsn(int opcode, String owner, String name, String desc) {
              super.visitFieldInsn(opcode, owner, name, desc);
              if (owner.equals(BridgeUtils.TRACED_METHOD_TYPE.getInternalName())) {
                logger.severe("Error in " + className + '.' + method);
                throw new IllegalInstructionException(BridgeUtils.TRACED_METHOD_TYPE.getClassName()
                                                              + '.' + name
                                                              + " can only be called from a traced "
                                                              + "method");
              }
            }

            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
              super.visitMethodInsn(opcode, owner, name, desc, itf);
              if (BridgeUtils.isAgentType(owner) && "getTracedMethod".equals(name)) {
                logger.severe("Error in " + className + '.' + method);
                throw new IllegalInstructionException(BridgeUtils.PUBLIC_AGENT_TYPE.getClassName() + '.'
                                                              + "getTracedMethod"
                                                              + " can only be called from a traced "
                                                              + "method");
              }
            }
          };
        }

        return mv;
      }
    };
  }

  static ClassVisitor removeDefaultConstructors(final Map<Method, MethodNode> constructors, final ClassVisitor cv) {
    return new ClassVisitor(Agent.ASM_LEVEL, cv) {
      public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                       String[] exceptions) {
        MethodNode methodNode = (MethodNode) constructors.get(new Method(name, desc));
        return this.isDefaultConstructor(methodNode) ? new MethodVisitor(Agent.ASM_LEVEL) {
        } : super.visitMethod(access, name, desc, signature, exceptions);
      }

      private boolean isDefaultConstructor(MethodNode methodNode) {
        return methodNode != null && methodNode.instructions.getLast().getPrevious().getOpcode() == 183;
      }
    };
  }

  private static ClassVisitor removeTraceAnnotationsFromMethods(final ClassVisitor cv) {
    return new ClassVisitor(Agent.ASM_LEVEL, cv) {
      public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                       String[] exceptions) {
        return new MethodVisitor(Agent.ASM_LEVEL, super.visitMethod(access, name, desc, signature, exceptions)) {
          public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return Type.getDescriptor(Trace.class).equals(desc) ? null
                           : super.visitAnnotation(desc, visible);
          }
        };
      }
    };
  }

  private static ClassVisitor fixInvocationInstructions(final Map<String, WeavedClassInfo> weaveClasses,
                                                        final ClassVisitor cv) {
    return new ClassVisitor(Agent.ASM_LEVEL, cv) {
      public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                       String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        final JSRInlinerAdapter mv1 = new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions);
        return new MethodVisitor(Agent.ASM_LEVEL, mv1) {
          public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (opcode == 182) {
              WeavedClassInfo weaveInfo = (WeavedClassInfo) weaveClasses.get(owner);
              if (weaveInfo != null && MatchType.Interface.equals(weaveInfo.getMatchType())) {
                itf = true;
                opcode = 185;
              }
            }

            super.visitMethodInsn(opcode, owner, name, desc, itf);
          }
        };
      }
    };
  }

  private String getFieldContainerKeyName(String className) {
    return this.instrumentationPackage.implementationTitle + ':' + className;
  }

  public String getClassName() {
    return this.className;
  }

  public boolean isSkipIfPresent() {
    return this.skipIfPresent;
  }

  public MatchType getMatchType() {
    return this.weaveAnnotation.getMatchType();
  }

  public boolean isWeaveInstrumentation() {
    return this.weaveAnnotation.getMatchType() != null;
  }

  private ClassVisitor verifyWeaveConstructors(final ClassVisitor cv) {
    return new ClassVisitor(Agent.ASM_LEVEL, cv) {
      public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                       String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ("<init>".equals(name) || "<clinit>".equals(name)) {
          mv = new MethodVisitor(Agent.ASM_LEVEL, mv) {
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
              if (BridgeUtils.WEAVER_TYPE.getInternalName().equals(owner)) {
                throw new IllegalInstructionException("Weave instrumentation constructors must not "
                                                              + "invoke " + BridgeUtils.WEAVER_TYPE
                                                                                    .getClassName()
                                                              + '.' + WeaveUtils.CALL_ORIGINAL_METHOD);
              } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
              }
            }
          };
        }

        return mv;
      }
    };
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ((64 & access) == 64) {
      return mv;
    } else if (this.isWeaveInstrumentation() && "<clinit>".equals(name)) {
      this.staticConstructor = new MethodNode(access, name, desc, signature, exceptions);
      return this.staticConstructor;
    } else {
      final Method method = new Method(name, desc);
      InstrumentationClassVisitor.TraceAnnotationVisitor mv1 =
              new InstrumentationClassVisitor.TraceAnnotationVisitor(mv, method);
      MethodVisitor mv2 = this.trackCatchAndLogAnnotations(mv1, method);
      if ((access & 1024) != 0) {
        return (MethodVisitor) mv2;
      } else {
        if ("<init>".equals(name)) {
          MethodNode node = new MethodNode(access, name, desc, signature, exceptions);
          mv2 = node;
          this.constructors.put(method, node);
        }

        return new MethodVisitor(Agent.ASM_LEVEL, (MethodVisitor) mv2) {
          public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (MergeMethodVisitor.isOriginalMethodInvocation(owner, name, desc)) {
              InstrumentationClassVisitor.this.weavedMethods.add(method);
            }

            super.visitMethodInsn(opcode, owner, name, desc, itf);
          }
        };
      }
    }
  }

  private MethodVisitor trackCatchAndLogAnnotations(final MethodVisitor mv, final Method method) {
    return new MethodVisitor(Agent.ASM_LEVEL, mv) {
      public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (Type.getDescriptor(CatchAndLog.class).equals(desc)) {
          InstrumentationClassVisitor.this.catchAndLogMethods.add(method);
        }

        return super.visitAnnotation(desc, visible);
      }
    };
  }

  public FieldVisitor visitField(final int access, final String name, final String desc, final String signature,
                                 final Object value) {
    if (this.isWeaveInstrumentation()) {
      if ((access & 8) == 8 && name.equals("serialVersionUID")) {
        String field1 =
                String.format("A static serialVersionUID field was declared in weaved class %s.  The weaver "
                                      + "does not support accessing serialVersionUID from weaeved classes.",
                                     new Object[] {this.className});
        this.getLogger().log(Level.SEVERE, field1);
        throw new IllegalInstructionException(field1);
      } else {
        FieldNode field = new FieldNode(Agent.ASM_LEVEL, access, name, desc, signature, value) {
          public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (WeaveUtils.NEW_FIELD_ANNOTATION_DESCRIPTOR.equals(desc)) {
              InstrumentationClassVisitor.this.newFields.put(this.name, this);
              if ((this.access & 8) == 0) {
                InstrumentationClassVisitor.this.hasNewInstanceField = true;
              }
            }

            return super.visitAnnotation(desc, visible);
          }
        };
        if ((access & 24) == 24) {
          this.newFields.put(name, field);
        } else {
          this.existingFields.put(name, field);
        }

        return field;
      }
    } else {
      return super.visitField(access, name, desc, signature, value);
    }
  }

  public void visitEnd() {
    super.visitEnd();
    this.existingFields.keySet().removeAll(this.newFields.keySet());
  }

  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    if (Type.getDescriptor(SkipIfPresent.class).equals(desc)) {
      this.skipIfPresent = true;
    }

    return this.weaveAnnotation.visitAnnotation(desc, visible, super.visitAnnotation(desc, visible));
  }

  public void visitInnerClass(String name, String outerName, String innerName, int access) {
    this.innerClasses.add(new InnerClassNode(name, outerName, innerName, access));
  }

  public Set<Method> getWeavedMethods() {
    return Collections.unmodifiableSet(this.weavedMethods);
  }

  public Map<Method, TraceDetails> getTracedMethods() {
    return Collections.unmodifiableMap(this.tracedMethods);
  }

  public Map<String, FieldNode> getNewFields() {
    return this.newFields;
  }

  public Collection<FieldNode> getReferencedFields() {
    return Collections.unmodifiableCollection(this.existingFields.values());
  }

  public void generateInitializeFieldHandlerInstructions(GeneratorAdapter generatorAdapter) {
    if (this.isWeaveInstrumentation()) {
      generatorAdapter.getStatic(BridgeUtils.AGENT_BRIDGE_TYPE, "objectFieldManager",
                                        Type.getType(ObjectFieldManager.class));
      generatorAdapter.push(this.getFieldContainerKeyName(this.className));
      generatorAdapter.loadThis();
      Type fieldContainerType = Type.getObjectType(getFieldContainerClassName(this.className));
      generatorAdapter.newInstance(fieldContainerType);
      generatorAdapter.dup();
      generatorAdapter.invokeConstructor(fieldContainerType, new Method("<init>", Type.VOID_TYPE, new Type[0]));
      generatorAdapter.invokeInterface(Type.getType(ObjectFieldManager.class), INITIALIZE_FIELDS_METHOD);
    }
  }

  private void generateVisitFieldInstructions(GeneratorAdapter generator, int opcode, String fieldName,
                                              String fieldDesc) {
    Type fieldContainerType = Type.getObjectType(getFieldContainerClassName(this.className));
    Type fieldType = Type.getType(fieldDesc);
    if (opcode == 180) {
      generator.getStatic(BridgeUtils.AGENT_BRIDGE_TYPE, "objectFieldManager",
                                 Type.getType(ObjectFieldManager.class));
      generator.swap();
      generator.push(this.getFieldContainerKeyName(this.className));
      generator.swap();
      generator.invokeInterface(Type.getType(ObjectFieldManager.class), GET_FIELD_CONTAINER_METHOD);
      this.verifyFieldContainerIsNotNull(generator);
      generator.checkCast(fieldContainerType);
      generator.getField(fieldContainerType, fieldName, fieldType);
    } else if (opcode == 181) {
      int fieldValue = generator.newLocal(fieldType);
      generator.storeLocal(fieldValue);
      generator.getStatic(BridgeUtils.AGENT_BRIDGE_TYPE, "objectFieldManager",
                                 Type.getType(ObjectFieldManager.class));
      generator.swap();
      generator.push(this.getFieldContainerKeyName(this.className));
      generator.swap();
      generator.invokeInterface(Type.getType(ObjectFieldManager.class), GET_FIELD_CONTAINER_METHOD);
      this.verifyFieldContainerIsNotNull(generator);
      generator.checkCast(fieldContainerType);
      generator.loadLocal(fieldValue);
      generator.putField(fieldContainerType, fieldName, Type.getType(fieldDesc));
    } else if (opcode == 178) {
      generator.getStatic(fieldContainerType, fieldName, fieldType);
    } else if (opcode == 179) {
      generator.putStatic(fieldContainerType, fieldName, Type.getType(fieldDesc));
    }

  }

  private void verifyFieldContainerIsNotNull(GeneratorAdapter generator) {
    generator.dup();
    Label skip = generator.newLabel();
    generator.ifNonNull(skip);
    generator.throwException(Type.getType(NullPointerException.class),
                                    "The object field container was null for " + this.getClassName());
    generator.visitLabel(skip);
  }

  public MethodVisitor getMethodVisitor(final String className, final MethodVisitor codeVisitor, final int access,
                                        final Method method) {
    if (!this.isWeaveInstrumentation()) {
      return codeVisitor;
    } else {
      MethodVisitor mv = codeVisitor;
      if (!this.newFields.isEmpty()) {
        mv = new GeneratorAdapter(Agent.ASM_LEVEL, codeVisitor, access, method.getName(), method.getDescriptor()) {
          public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            FieldNode fieldNode = (FieldNode) InstrumentationClassVisitor.this.newFields.get(name);
            if (null != fieldNode) {
              InstrumentationClassVisitor.this.generateVisitFieldInstructions(this, opcode, name, desc);
            } else {
              super.visitFieldInsn(opcode, owner, name, desc);
            }

          }
        };
      }

      if (!this.existingFields.isEmpty()) {
        mv = new GeneratorAdapter(Agent.ASM_LEVEL, (MethodVisitor) mv, access, method.getName(),
                                         method.getDescriptor()) {
          public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (className.equals(owner) && !InstrumentationClassVisitor.this.newFields.containsKey(name)) {
              FieldNode existingField =
                      (FieldNode) InstrumentationClassVisitor.this.existingFields.get(name);
              if (existingField == null) {
                throw new IllegalInstructionException("Weaved instrumentation method " + method
                                                              + " is attempting to access field " + name
                                                              + " of type " + desc
                                                              + " which does not exist.  This field "
                                                              + "may need to be marked with "
                                                              + NewField.class.getName());
              }

              if (!desc.equals(existingField.desc)) {
                throw new IllegalInstructionException("Weaved instrumentation method " + method
                                                              + " accesses field " + name + " of type "
                                                              + desc + ", but the actual type is "
                                                              + existingField.desc);
              }
            }

            super.visitFieldInsn(opcode, owner, name, desc);
          }
        };
      }

      return (MethodVisitor) mv;
    }
  }

  public MethodVisitor getConstructorMethodVisitor(MethodVisitor mv, String className, final int access,
                                                   final String name, final String desc) {
    if (this.hasNewInstanceField) {
      mv = new AdviceAdapter(Agent.ASM_LEVEL, (MethodVisitor) mv, access, name, desc) {
        protected void onMethodExit(int opcode) {
          if (opcode != 191) {
            Label start = this.newLabel();
            Label end = this.newLabel();
            Label handler = this.newLabel();
            this.visitLabel(start);
            InstrumentationClassVisitor.this.generateInitializeFieldHandlerInstructions(this);
            this.goTo(end);
            this.visitLabel(handler);
            this.pop();
            this.visitLabel(end);
            this.visitTryCatchBlock(start, end, handler, Type.getType(Throwable.class).getInternalName());
          }

          super.onMethodExit(opcode);
        }
      };
    }

    return (MethodVisitor) mv;
  }

  ClassVisitor createSecondPassVisitor(InstrumentationPackage instrumentationPackage, Map<String, byte[]> classBytes,
                                       List<String> newClassLoadOrder, Map<String, WeavedClassInfo> weaveClasses,
                                       ClassReader reader, ClassVisitor cv) {
    SecurityManager securityManager = System.getSecurityManager();
    if (securityManager != null && !this.isWeaveInstrumentation()) {
      cv = this.handleElevatePermissions(cv, classBytes);
    }

    LogApiCallsVisitor cv1 = new LogApiCallsVisitor(instrumentationPackage, cv);
    RegisterClosableInstrumentationVisitor cv2 =
            new RegisterClosableInstrumentationVisitor(instrumentationPackage, cv1);
    cv = fixInvocationInstructions(weaveClasses, cv2);
    cv = enforceTracedMethodsAccessingTracedMethod(cv, this.getLogger(), this.className,
                                                          this.tracedMethods.keySet());
    if (this.isWeaveInstrumentation()) {
      if (this.skipIfPresent) {
        ;
      }

      this.createFieldContainerClass(classBytes);
      LogWeavedMethodInvocationsVisitor cv3 = new LogWeavedMethodInvocationsVisitor(instrumentationPackage, cv);
      cv = removeTraceAnnotationsFromMethods(cv3);
      cv = this.verifyWeaveConstructors(cv);
      cv = removeDefaultConstructors(this.constructors, cv);
      if (!this.catchAndLogMethods.isEmpty()) {
        this.getLogger().log(Level.SEVERE,
                                    "{0} is a weaved class but the following methods are marked with the {1} "
                                            + "annotation: {2}",
                                    new Object[] {this.className, CatchAndLog.class.getSimpleName(),
                                                         this.catchAndLogMethods});
      }
    } else {
      this.verifyNewClass();
      NewClassDependencyVisitor dependencyCV = new NewClassDependencyVisitor(Agent.ASM_LEVEL, cv, newClassLoadOrder);
      cv = NewClassMarker.getVisitor(dependencyCV, instrumentationPackage.implementationTitle,
                                            Float.toString(instrumentationPackage.implementationVersion));
      cv = CurrentTransactionRewriter.rewriteCurrentTransactionReferences(cv, reader);
      if (!this.catchAndLogMethods.isEmpty()) {
        cv = this.rewriteCatchAndLogMethods(cv);
      }
    }

    return cv;
  }

  private ClassVisitor rewriteCatchAndLogMethods(final ClassVisitor cv) {
    return new ClassVisitor(Agent.ASM_LEVEL, cv) {
      public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                       String[] exceptions) {
        Object mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (InstrumentationClassVisitor.this.catchAndLogMethods.contains(new Method(name, desc))) {
          if (Type.VOID_TYPE.equals(Type.getType(desc).getReturnType())) {
            mv = InstrumentationClassVisitor
                         .catchAndLogMethodExceptions(InstrumentationClassVisitor.this
                                                              .instrumentationPackage
                                                              .getImplementationTitle(),
                                                             InstrumentationClassVisitor.this.className,
                                                             access, name, desc, (MethodVisitor) mv);
          } else {
            InstrumentationClassVisitor.this.getLogger().log(Level.SEVERE,
                                                                    "{0}.{1}{2} is marked with {3}, but "
                                                                            + "only void return types are"
                                                                            + " supported.",
                                                                    new Object[]
                                                                            {InstrumentationClassVisitor
                                                                                     .this.className,
                                                                                    name, desc,
                                                                                    CatchAndLog.class
                                                                                            .getSimpleName()});
          }
        }

        return (MethodVisitor) mv;
      }
    };
  }

  private ClassVisitor handleElevatePermissions(final ClassVisitor cv, Map<String, byte[]> classBytes) {
    return new ClassVisitor(Agent.ASM_LEVEL, cv) {
      private final ReflectionHelper reflection = ReflectionHelper.get();

      public MethodVisitor visitMethod(final int access, final String methodName, final String methodDesc,
                                       String signature, String[] exceptions) {
        final MethodVisitor mv = super.visitMethod(access, methodName, methodDesc, signature, exceptions);
        return new GeneratorAdapter(Agent.ASM_LEVEL, mv, access, methodName, methodDesc) {
          public void visitLdcInsn(Object cst) {
            if (cst instanceof Type && !InstrumentationClassVisitor.this.getClassName()
                                                .equals(((Type) cst).getInternalName())) {
              this.loadClass((Type) cst);
            } else {
              super.visitLdcInsn(cst);
            }

          }

          private void loadClass(Type typeToLoad) {
            super.visitLdcInsn(Type.getObjectType(InstrumentationClassVisitor.this.className));
            super.invokeStatic(Type.getType(ClassReflection.class), new Method("getClassLoader",
                                                                                      "(Ljava/lang/Class;"
                                                                                              + ")Ljava/lang/ClassLoader;"));
            this.push(typeToLoad.getClassName());
            super.invokeStatic(Type.getType(ClassReflection.class), new Method("loadClass",
                                                                                      "(Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/lang/Class;"));
          }

          public void visitTypeInsn(int opcode, String type) {
            Type objectType = Type.getObjectType(type);
            if (193 == opcode) {
              this.loadClass(objectType);
              super.swap();
              super.invokeVirtual(Type.getType(Class.class),
                                         new Method("isInstance", "(Ljava/lang/Object;)Z"));
            } else if (192 == opcode) {
              if (!type.startsWith("java/")) {
                this.addWeaveClass(objectType);
              }

              super.visitTypeInsn(opcode, type);
            } else {
              super.visitTypeInsn(opcode, type);
            }

          }

          private void addWeaveClass(Type objectType) {
            ((InstrumentationImpl) AgentBridge.instrumentation).addWeaveClass(objectType);
          }

          public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (!reflection.process(owner, name, desc, this)) {
              this.addWeaveClass(Type.getObjectType(owner));
              super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
          }
        };
      }
    };
  }

  void verifyNewClass() throws IllegalInstructionException {
    if (!this.newFields.isEmpty()) {
      this.getLogger()
              .severe("Non-weave class " + this.className + " cannot use @NewField for fields " + this.newFields
                                                                                                          .keySet());
      throw new IllegalInstructionException(BridgeUtils.WEAVER_TYPE.getClassName()
                                                    + " is not a weaved method but uses the @NewField "
                                                    + "annotation");
    } else if (!this.weavedMethods.isEmpty()) {
      this.getLogger().severe("Error in " + this.className + " methods " + this.weavedMethods);
      throw new IllegalInstructionException(BridgeUtils.WEAVER_TYPE.getClassName() + '.'
                                                    + WeaveUtils.CALL_ORIGINAL_METHOD
                                                    + " can only be called from a weaved method");
    } else if (!this.tracedMethods.isEmpty()) {
      this.getLogger().severe("Error in " + this.className + " methods " + this.tracedMethods.keySet());
      throw new IllegalInstructionException("The Trace annotation can only be used on existing methods in "
                                                    + "existing classes");
    }
  }

  private void createFieldContainerClass(Map<String, byte[]> classBytes) {
    if (!this.newFields.isEmpty()) {
      final String className = getFieldContainerClassName(this.className);
      AgentBridge.objectFieldManager.createClassObjectFields(this.getFieldContainerKeyName(this.className));
      ClassWriter cw = new ClassWriter(1);
      cw.visit(49, 33, className, (String) null, "java/lang/Object", new String[0]);
      Iterator mv = this.newFields.values().iterator();

      while (mv.hasNext()) {
        FieldNode field = (FieldNode) mv.next();
        field.access |= 1;
        field.access &= -23;
        cw.visitField(field.access, field.name, field.desc, field.signature, field.value);
      }

      MethodVisitor mv1;
      if (this.staticConstructor != null) {
        mv1 = cw.visitMethod(9, this.staticConstructor.name, this.staticConstructor.desc, (String) null,
                                    (String[]) null);
        mv1 = new MethodVisitor(Agent.ASM_LEVEL, mv1) {
          public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (owner.equals(InstrumentationClassVisitor.this.className)
                        && InstrumentationClassVisitor.this.newFields.containsKey(name)) {
              owner = className;
            }

            super.visitFieldInsn(opcode, owner, name, desc);
          }
        };
        this.staticConstructor.instructions.accept(mv1);
        mv1.visitMaxs(1, 1);
        mv1.visitEnd();
      }

      mv1 = cw.visitMethod(1, "<init>", "()V", (String) null, (String[]) null);
      mv1.visitVarInsn(25, 0);
      mv1.visitMethodInsn(183, "java/lang/Object", "<init>", "()V", false);
      mv1.visitInsn(177);
      mv1.visitMaxs(1, 1);
      mv1.visitEnd();
      cw.visitEnd();
      this.getLogger().finest("Generated field container " + className);
      classBytes.put(className, cw.toByteArray());
    }
  }

  private IAgentLogger getLogger() {
    return this.instrumentationPackage.getLogger();
  }

  public String getSuperName() {
    return this.superName;
  }

  private class TraceAnnotationVisitor extends MethodVisitor {
    private final Method method;

    public TraceAnnotationVisitor(MethodVisitor mv, Method method) {
      super(Agent.ASM_LEVEL, mv);
      this.method = method;
    }

    public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
      final AnnotationVisitor av = super.visitAnnotation(desc, visible);
      if (Type.getDescriptor(Trace.class).equals(desc)) {
        final TraceDetailsBuilder builder = TraceDetailsBuilder.newBuilder()
                                                    .setInstrumentationType(InstrumentationType
                                                                                    .WeaveInstrumentation)
                                                    .setInstrumentationSourceName(InstrumentationClassVisitor
                                                                                          .this
                                                                                          .instrumentationPackage.implementationTitle);
        return new Annotation(av, desc, builder) {
          public void visitEnd() {
            InstrumentationClassVisitor.this.tracedMethods
                    .put(TraceAnnotationVisitor.this.method, this.getTraceDetails(false));
            super.visitEnd();
          }
        };
      } else {
        return av;
      }
    }
  }
}
