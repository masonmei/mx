//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.weaver;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;

import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.ClassWriter;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.Method;
import com.newrelic.deps.org.objectweb.asm.commons.RemappingClassAdapter;
import com.newrelic.deps.org.objectweb.asm.commons.SimpleRemapper;
import com.newrelic.deps.org.objectweb.asm.tree.InnerClassNode;

import com.newrelic.deps.com.google.common.collect.ImmutableMap;
import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.config.Config;
import com.newrelic.agent.instrumentation.classmatchers.ChildClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.DefaultClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher.Match;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcherBuilder;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.BootstrapLoader;
import com.newrelic.agent.util.Streams;
import com.newrelic.agent.util.asm.ClassResolver;
import com.newrelic.agent.util.asm.ClassResolvers;
import com.newrelic.agent.util.asm.PatchedClassWriter;
import com.newrelic.api.agent.weaver.MatchType;

public class InstrumentationPackage implements ClassResolver {
  public final List<String> newClassLoadOrder = Lists.newLinkedList();
  final String implementationTitle;
  final Map<String, byte[]> newClasses = Maps.newHashMap();
  final float implementationVersion;
  private final Verifier verifier;
  private final Map<Method, String> abstractMethods = Maps.newHashMap();
  private final Map<String, byte[]> classNames;
  private final Map<String, WeavedClassInfo> weaveClasses = Maps.newHashMap();
  private final Map<String, WeavedClassInfo> instrumentationInfo = Maps.newHashMap();
  private final Set<String> skipClasses = Sets.newHashSet();
  private final boolean containsBootstrapMergeClasses;
  private final ClassAppender classAppender;
  private final InstrumentationMetadata metaData;
  private final Instrumentation instrumentation;
  private final Collection<Closeable> closeables = new ConcurrentLinkedQueue();
  private final ClassMatchVisitorFactory matcher;
  private final String location;
  private final IAgentLogger logger;
  private OptimizedClassMatcherBuilder matcherBuilder = OptimizedClassMatcherBuilder.newBuilder();

  public InstrumentationPackage(Instrumentation instrumentation, IAgentLogger logger,
                                InstrumentationMetadata metaData, JarInputStream jarStream) throws Exception {
    this.location = metaData.getLocation();
    this.instrumentation = instrumentation;
    HashMap classBytes = Maps.newHashMap();
    HashMap instrumentationClasses = Maps.newHashMap();
    this.implementationTitle = metaData.getImplementationTitle();
    this.metaData = metaData;
    this.logger = logger;
    this.implementationVersion = metaData.getImplementationVersion();
    this.verifier = new Verifier(this);
    JarEntry entry = null;

    while ((entry = jarStream.getNextJarEntry()) != null) {
      if (entry.getName().endsWith(".class")) {
        byte[] bytes = Streams.read(jarStream, (int) entry.getSize(), false);
        InstrumentationClassVisitor instrumentationClass =
                InstrumentationClassVisitor.getInstrumentationClass(this, bytes);
        instrumentationClasses.put(instrumentationClass.getClassName(), instrumentationClass);
        MatchType matchType = instrumentationClass.getMatchType();
        if (matchType != null) {
          this.weaveClasses.put(instrumentationClass.getClassName(), instrumentationClass);
        }

        this.instrumentationInfo.put(instrumentationClass.getClassName(), instrumentationClass);
        logger.finest("Weave instrumentation class: " + instrumentationClass.getClassName() + ", type: "
                              + (matchType == null ? "NewClass" : matchType));
        classBytes.put(instrumentationClass.getClassName(), bytes);
      }
    }

    this.newClassLoadOrder.addAll(instrumentationClasses.keySet());
    this.newClassLoadOrder.removeAll(this.weaveClasses.keySet());
    if (this.weaveClasses.isEmpty()) {
      logger.finer(this.implementationTitle + " does not contain any weaved classes.");
    }

    InstrumentationClassVisitor
            .performSecondPassProcessing(this, instrumentationClasses, this.weaveClasses, classBytes,
                                                this.newClassLoadOrder);
    this.classNames = ImmutableMap.copyOf(this.performThirdPassProcessing(classBytes, instrumentationClasses));
    this.containsBootstrapMergeClasses = this.isBootstrapClassName(this.weaveClasses.keySet());
    if (this.containsBootstrapMergeClasses) {
      this.classAppender = ClassAppender.getBootstrapClassAppender(instrumentation);
    } else {
      this.classAppender = ClassAppender.getSystemClassAppender();
    }

    this.matcher = this.matcherBuilder.build();
    this.matcherBuilder = null;
  }

  static ClassMatcher getClassMatcher(MatchType type, String className) {
    switch (type.ordinal()) {
      case 1:
        return new InterfaceMatcher(className);
      case 2:
        return new ChildClassMatcher(className, false);
      default:
        return new ExactClassMatcher(className);
    }
  }

  private Map<String, byte[]> performThirdPassProcessing(Map<String, byte[]> classBytes,
                                                         Map<String, InstrumentationClassVisitor>
                                                                 instrumentationClasses) {
    HashMap renamedClasses = Maps.newHashMap();
    Iterator i$ = instrumentationClasses.values().iterator();

    while (true) {
      InstrumentationClassVisitor instrumentationClass;
      do {
        if (!i$.hasNext()) {
          return this.renameClasses(classBytes, renamedClasses, instrumentationClasses);
        }

        instrumentationClass = (InstrumentationClassVisitor) i$.next();
      } while (!instrumentationClass.isWeaveInstrumentation());

      Iterator i$1 = instrumentationClass.innerClasses.iterator();

      while (i$1.hasNext()) {
        InnerClassNode innerClass = (InnerClassNode) i$1.next();
        InstrumentationClassVisitor innerClassInfo =
                (InstrumentationClassVisitor) instrumentationClasses.get(innerClass.name);
        if (innerClassInfo != null && !innerClassInfo.isWeaveInstrumentation()) {
          renamedClasses.put(innerClass.name, innerClass.name + "$NR");
        }
      }
    }
  }

  private Map<String, byte[]> renameClasses(Map<String, byte[]> classBytes, Map<String, String> classesToRename,
                                            Map<String, InstrumentationClassVisitor> instrumentationClasses) {
    HashMap actualClassNames = Maps.newHashMap();
    HashMap referencedClassMethods = Maps.newHashMap();
    HashMap referencedInterfaceMethods = Maps.newHashMap();
    SimpleRemapper remapper = new SimpleRemapper(classesToRename);
    Iterator i$ = classBytes.entrySet().iterator();

    while (true) {
      while (i$.hasNext()) {
        Entry entry = (Entry) i$.next();
        ClassReader reader = new ClassReader((byte[]) entry.getValue());
        ClassWriter writer = new ClassWriter(1);
        Object cv = writer;
        WeavedClassInfo instrumentationClass =
                (WeavedClassInfo) this.instrumentationInfo.get(reader.getClassName());
        boolean isWeaveClass = instrumentationClass != null && instrumentationClass.getMatchType() != null;
        if (isWeaveClass) {
          cv = new InstrumentationPackage.GatherClassMethodMatchers(writer, reader.getClassName(),
                                                                           instrumentationClass);
        }

        cv = new ReferencesVisitor(this.logger, this.getWeavedClassDetails(reader.getClassName()),
                                          (ClassVisitor) cv, referencedClassMethods,
                                          referencedInterfaceMethods);
        if (!classesToRename.isEmpty()) {
          cv = new RemappingClassAdapter((ClassVisitor) cv, remapper);
        }

        reader.accept((ClassVisitor) cv, 8);
        String className = (String) classesToRename.get(entry.getKey());
        if (className == null) {
          className = (String) entry.getKey();
        }

        actualClassNames.put(className, writer.toByteArray());
        if (instrumentationClass != null && instrumentationClass.isSkipIfPresent()) {
          this.skipClasses.add(className);
        } else if (!isWeaveClass) {
          this.newClasses.put(className, writer.toByteArray());
        }
      }

      this.verifier.setReferences(referencedClassMethods, referencedInterfaceMethods);
      return actualClassNames;
    }
  }

  protected boolean loadClasses(ClassLoader loader, Map<String, URL> resolvedClasses) {
    Iterator i$ = resolvedClasses.keySet().iterator();

    while (i$.hasNext()) {
      String className = (String) i$.next();

      try {
        loader.loadClass(Type.getObjectType(className).getClassName());
      } catch (ClassNotFoundException var6) {
        this.logger.log(Level.FINER, "Error loading classes for {0} ({1}) : {2}",
                               new Object[] {this.metaData.getImplementationTitle(), className,
                                                    var6.getMessage()});
        return false;
      }
    }

    return true;
  }

  private boolean isBootstrapClassName(Collection<String> names) {
    BootstrapLoader bootstrapLoader = BootstrapLoader.get();
    Iterator i$ = names.iterator();

    String name;
    do {
      if (!i$.hasNext()) {
        return false;
      }

      name = (String) i$.next();
    } while (!bootstrapLoader.isBootstrapClass(name));

    return true;
  }

  public Verifier getVerifier() {
    return this.verifier;
  }

  public IAgentLogger getLogger() {
    return this.logger;
  }

  public String getImplementationTitle() {
    return this.implementationTitle;
  }

  public float getImplementationVersion() {
    return this.implementationVersion;
  }

  public String getLocation() {
    return this.location;
  }

  public void addClassMethodMatcher(ClassAndMethodMatcher classAndMethodMatcher, String className) {
    this.matcherBuilder.addClassMethodMatcher(new ClassAndMethodMatcher[] {classAndMethodMatcher});
  }

  public void addCloseable(Closeable closeable) {
    this.closeables.add(closeable);
  }

  public Collection<Closeable> getCloseables() {
    return Collections.unmodifiableCollection(this.closeables);
  }

  public ClassMatchVisitorFactory getMatcher() {
    return this.matcher;
  }

  public boolean matches(String className) {
    return this.classNames.keySet().contains(className);
  }

  public boolean containsAbstractMatchers() {
    return !this.abstractMethods.isEmpty();
  }

  public boolean isWeaved(String className) {
    return this.weaveClasses.containsKey(className);
  }

  public MixinClassVisitor getMixin(String className) throws IOException {
    WeavedClassInfo weavedClassInfo = (WeavedClassInfo) this.weaveClasses.get(className);
    if (weavedClassInfo == null) {
      return null;
    } else {
      byte[] bytes = (byte[]) this.classNames.get(className);
      if (bytes != null) {
        ClassReader classReader = new ClassReader(bytes);
        MixinClassVisitor cv = new MixinClassVisitor(bytes, this, weavedClassInfo);
        classReader.accept(cv, 8);
        if (this.metaData.isDebug()) {
          cv.print();
        }

        return cv;
      } else {
        return null;
      }
    }
  }

  public Map<String, byte[]> getClassBytes() {
    return this.classNames;
  }

  public boolean containsJDKClasses() {
    Iterator i$ = this.getClassBytes().keySet().iterator();

    String className;
    do {
      if (!i$.hasNext()) {
        return false;
      }

      className = (String) i$.next();
    } while (!className.startsWith("java/") && !className.startsWith("sun/"));

    return true;
  }

  public Set<String> getClassNames() {
    HashSet names = Sets.newHashSet();
    Iterator i$ = this.getClassBytes().keySet().iterator();

    while (i$.hasNext()) {
      String name = (String) i$.next();
      names.add(Type.getObjectType(name).getClassName());
    }

    return names;
  }

  public ClassAppender getClassAppender() {
    return this.classAppender;
  }

  public String getClassMatch(Match match) {
    Iterator i$ = match.getClassMatches().values().iterator();

    while (i$.hasNext()) {
      Collection classNames = (Collection) i$.next();
      Iterator i$1 = classNames.iterator();

      while (i$1.hasNext()) {
        String className = (String) i$1.next();
        if (this.classNames.get(className) != null) {
          return className;
        }
      }
    }

    return null;
  }

  public boolean isEnabled() {
    Config config = ServiceFactory.getConfigService().getDefaultAgentConfig().getClassTransformerConfig()
                            .getInstrumentationConfig(this.implementationTitle);
    if (!((Boolean) config.getProperty("enabled", Boolean.valueOf(this.metaData.isEnabled()))).booleanValue()) {
      this.logger.log(Level.FINE, "Disabled instrumentation \"{0}\"", new Object[] {this.implementationTitle});
      return false;
    } else {
      return true;
    }
  }

  public String toString() {
    return this.implementationTitle + " instrumentation";
  }

  public WeavedClassInfo getWeavedClassDetails(String internalName) {
    return (WeavedClassInfo) this.weaveClasses.get(internalName);
  }

  public Map<String, WeavedClassInfo> getWeaveClasses() {
    return this.weaveClasses;
  }

  public ClassWriter getClassWriter(int flags, ClassLoader loader) {
    ClassResolver classResolver = ClassResolvers.getMultiResolver(new ClassResolver[] {this, ClassResolvers
                                                                                                     .getClassLoaderResolver(loader)});
    return new PatchedClassWriter(2, classResolver);
  }

  public InputStream getClassResource(String internalName) throws IOException {
    byte[] bytes = (byte[]) this.newClasses.get(internalName);
    return bytes != null ? new ByteArrayInputStream(bytes) : null;
  }

  public MixinClassVisitor getMixinClassVisitor(String... matchClassNames) throws IOException {
    String[] arr$ = matchClassNames;
    int len$ = matchClassNames.length;

    for (int i$ = 0; i$ < len$; ++i$) {
      String matchClassName = arr$[i$];
      MixinClassVisitor mixin = this.getMixin(matchClassName);
      if (mixin != null) {
        return mixin;
      }
    }

    return null;
  }

  Set<String> getSkipClasses() {
    return this.skipClasses;
  }

  private class GatherClassMethodMatchers extends ClassVisitor {
    private final List<Method> methods = Lists.newArrayList();
    private final String className;
    private final MatchType matchType;
    private final WeavedClassInfo instrumentationClass;

    public GatherClassMethodMatchers(ClassVisitor cv, String className, WeavedClassInfo instrumentationClass) {
      super(Agent.ASM_LEVEL, cv);
      this.className = className;
      this.matchType = instrumentationClass.getMatchType();
      this.instrumentationClass = instrumentationClass;
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      final Method method = new Method(name, desc);
      final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
      if (OptimizedClassMatcher.DEFAULT_CONSTRUCTOR.getName().equals(name)
                  && !OptimizedClassMatcher.DEFAULT_CONSTRUCTOR.getDescriptor().equals(desc)) {
        this.methods.add(method);
        return mv;
      } else {
        return new MethodVisitor(Agent.ASM_LEVEL, mv) {
          public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (MergeMethodVisitor.isOriginalMethodInvocation(owner, name, desc)) {
              GatherClassMethodMatchers.this.methods.add(method);
            }

            super.visitMethodInsn(opcode, owner, name, desc, itf);
          }
        };
      }
    }

    public void visitEnd() {
      super.visitEnd();
      Object methods = this.methods;
      ((List) methods).remove(OptimizedClassMatcher.DEFAULT_CONSTRUCTOR);
      if (((List) methods).isEmpty()) {
        if (this.instrumentationClass.getTracedMethods().isEmpty()) {
          InstrumentationPackage.this.logger.fine(this.className
                                                          + " is marked as a weaved class, but no methods are matched to be weaved.");
          return;
        }

        methods = Lists.newArrayList(this.instrumentationClass.getTracedMethods().keySet());
      }

      ClassMatcher classMatcher = InstrumentationPackage.getClassMatcher(this.matchType, this.className);

      Method m;
      for (Iterator i$ = ((List) methods).iterator(); i$.hasNext(); InstrumentationPackage.this
                                                                            .addClassMethodMatcher(new DefaultClassAndMethodMatcher(classMatcher,
                                                                                                                                           new ExactMethodMatcher(m.getName(),
                                                                                                                                                                         m.getDescriptor())),
                                                                                                          this.className)) {
        m = (Method) i$.next();
        if (!this.matchType.isExactMatch()) {
          InstrumentationPackage.this.abstractMethods.put(m, this.className);
        }
      }

    }
  }
}
