package com.newrelic.agent.instrumentation;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import com.newrelic.deps.org.objectweb.asm.commons.Method;

import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.instrumentation.classmatchers.ClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcherBuilder;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.ContextClassTransformer;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.agent.instrumentation.custom.ClassRetransformer;
import com.newrelic.agent.instrumentation.pointcuts.database.ConnectionClassTransformer;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.DefaultThreadFactory;

public class ClassTransformerServiceImpl extends AbstractService implements ClassTransformerService {
  private final boolean isEnabled;
  private final List<ClassFileTransformer> classTransformers =
          Collections.synchronizedList(new ArrayList<ClassFileTransformer>());
  private final long shutdownTime;
  private final InstrumentationImpl instrumentation;
  private final ScheduledExecutorService executor;
  private final Instrumentation extensionInstrumentation;
  private final AtomicReference<Set<ClassMatchVisitorFactory>> retransformClassMatchers =
          new AtomicReference<Set<ClassMatchVisitorFactory>>(createRetransformClassMatcherList());
  private volatile ClassTransformer classTransformer;
  private volatile ClassRetransformer localRetransformer;
  private volatile ClassRetransformer remoteRetransformer;
  private InstrumentationContextManager contextManager;
  private ClassTransformerServiceImpl.TraceMatchTransformer traceMatchTransformer;

  public ClassTransformerServiceImpl(InstrumentationProxy instrumentationProxy) throws Exception {
    super(ClassTransformerServiceImpl.class.getSimpleName());
    this.extensionInstrumentation = new ExtensionInstrumentation(instrumentationProxy);
    this.instrumentation = new InstrumentationImpl(this.logger);
    AgentBridge.instrumentation = this.instrumentation;
    DefaultThreadFactory factory = new DefaultThreadFactory("New Relic Retransformer", true);
    this.executor = Executors.newSingleThreadScheduledExecutor(factory);
    AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
    this.isEnabled = config.getClassTransformerConfig().isEnabled();
    long shutdownDelayInNanos = config.getClassTransformerConfig().getShutdownDelayInNanos();
    if (shutdownDelayInNanos > 0L) {
      this.shutdownTime = System.nanoTime() + shutdownDelayInNanos;
      String msg = MessageFormat
                           .format("The Class Transformer Service will stop instrumenting classes after {0} secs",
                                          TimeUnit.SECONDS.convert(shutdownDelayInNanos, TimeUnit.NANOSECONDS));
      this.getLogger().info(msg);
    } else {
      this.shutdownTime = Long.MAX_VALUE;
    }
  }

  private Set<ClassMatchVisitorFactory> createRetransformClassMatcherList() {
    return Sets.newSetFromMap(Maps.<ClassMatchVisitorFactory, Boolean>newConcurrentMap());
  }

  protected void doStart() throws Exception {
    if (!isEnabled()) {
      getLogger().info("The class transformer is disabled.  No classes will be instrumented.");
      return;
    }
    InstrumentationProxy instrProxy = ServiceFactory.getAgent().getInstrumentation();
    if (instrProxy == null) {
      getLogger().severe("Unable to initialize the class transformer because there is no instrumentation hook");
    } else {
      classTransformer = startClassTransformer(instrProxy);
    }
    executor.schedule(new Runnable() {
      public void run() {
        retransformMatchingClasses();
      }
    }, getRetransformPeriodInSeconds(), TimeUnit.SECONDS);
  }

  private long getRetransformPeriodInSeconds() {
    return ServiceFactory.getConfigService().getDefaultAgentConfig()
                   .getValue("class_transformer.retransformation_period", 10L);
  }

  public void checkShutdown() {
    if ((shutdownTime == Long.MAX_VALUE) || (isStopped())) {
      return;
    }

    long nsTilShutdown = shutdownTime - System.nanoTime();
    if (nsTilShutdown < 0L) {
      try {
        getLogger().info("Stopping Class Transformer Service based on configured shutdown_delay");
        stop();
      } catch (Exception e) {
        String msg = MessageFormat.format("Failed to stop Class Transformer Service: {0}", e);
        getLogger().error(msg);
      }
    }
  }

  private ClassTransformer startClassTransformer(InstrumentationProxy instrProxy) throws Exception {
    boolean retransformSupported = isRetransformationSupported(instrProxy);

    ClassTransformer classTransformer = new ClassTransformer(instrProxy, retransformSupported);
    contextManager = InstrumentationContextManager.create(instrProxy, AgentBridge.class.getClassLoader() == null);

    contextManager.addContextClassTransformer(classTransformer.getMatcher(), classTransformer);
    for (PointCut pc : classTransformer.getPointcuts()) {
      Agent.LOG.log(Level.FINEST, "pointcut {0} active", pc);
      pc.noticeTransformerStarted(classTransformer);
    }

    localRetransformer = new ClassRetransformer(contextManager);
    localRetransformer.setClassMethodMatchers(ServiceFactory.getExtensionService().getEnabledPointCuts());

    remoteRetransformer = new ClassRetransformer(contextManager);

    traceMatchTransformer = new TraceMatchTransformer(contextManager);

    StartableClassFileTransformer[] startableClassTransformers =
            new StartableClassFileTransformer[] {
                                                        new InterfaceMixinClassTransformer(classTransformer.getClassReaderFlags()),
                                                        new JDBCClassTransformer(classTransformer),
                                                        new ConnectionClassTransformer(classTransformer)
            };

    for (StartableClassFileTransformer transformer : startableClassTransformers) {
      transformer.start(instrProxy, retransformSupported);
      classTransformers.add(transformer);
    }
    StartableClassFileTransformer[] classTransformers =
            InterfaceImplementationClassTransformer.getClassTransformers(classTransformer);
    for (StartableClassFileTransformer transformer : classTransformers) {
      transformer.start(instrProxy, retransformSupported);
      this.classTransformers.add(transformer);
    }
    return classTransformer;
  }

  private boolean isRetransformationSupported(InstrumentationProxy instrProxy) {
    AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
    Boolean enableClassRetransformation = config.getProperty("enable_class_retransformation");
    if (enableClassRetransformation != null) {
      return enableClassRetransformation;
    }
    try {
      return instrProxy.isRetransformClassesSupported();
    } catch (Exception e) {
      String msg = MessageFormat.format("Unexpected error asking current JVM configuration if it supports "
                                                + "retransformation of classes: {0}", e);

      getLogger().warning(msg);
    }
    return false;
  }

  private void retransformMatchingClasses() {
    Set<ClassMatchVisitorFactory> matchers =
            retransformClassMatchers.getAndSet(createRetransformClassMatcherList());

    if (!matchers.isEmpty()) {
      retransformMatchingClassesImmediately(matchers);
    }
  }

  public void retransformMatchingClasses(Collection<ClassMatchVisitorFactory> matchers) {
    retransformClassMatchers.get().addAll(matchers);
  }

  public void retransformMatchingClassesImmediately(Collection<ClassMatchVisitorFactory> matchers) {
    InstrumentationProxy instrumentation = ServiceFactory.getAgent().getInstrumentation();
    Class<?>[] allLoadedClasses = instrumentation.getAllLoadedClasses();
    Set<Class<?>> classesToRetransform = InstrumentationContext.getMatchingClasses(matchers, allLoadedClasses);

    if (!classesToRetransform.isEmpty()) {
      try {
        instrumentation
                .retransformClasses(classesToRetransform.toArray(new Class<?>[classesToRetransform.size()]));
      } catch (UnmodifiableClassException e) {
        logger.log(Level.FINER, "Error retransforming classes: " + classesToRetransform, e);
      }
    }
  }

  protected void doStop() throws Exception {
    executor.shutdown();

    InstrumentationProxy instrProxy = ServiceFactory.getAgent().getInstrumentation();
    if (instrProxy == null) {
      return;
    }
    for (ClassFileTransformer classFileTransformer : classTransformers) {
      instrProxy.removeTransformer(classFileTransformer);
    }
  }

  public InstrumentationContextManager getContextManager() {
    return contextManager;
  }

  public ClassTransformer getClassTransformer() {
    return classTransformer;
  }

  public ClassRetransformer getLocalRetransformer() {
    return localRetransformer;
  }

  public ClassRetransformer getRemoteRetransformer() {
    return remoteRetransformer;
  }

  public boolean isEnabled() {
    return isEnabled;
  }

  public boolean addTraceMatcher(ClassAndMethodMatcher matcher, String metricPrefix) {
    return traceMatchTransformer.addTraceMatcher(matcher, metricPrefix);
  }

  public Instrumentation getExtensionInstrumentation() {
    return extensionInstrumentation;
  }

  private static class TraceMatchTransformer implements ContextClassTransformer {
    private final Map<ClassAndMethodMatcher, String> matchersPrefix = Maps.newConcurrentMap();
    private final Set<ClassMatchVisitorFactory> matchVisitors = Sets.newConcurrentHashSet();
    private final InstrumentationContextManager contextManager;

    TraceMatchTransformer(InstrumentationContextManager manager) {
      contextManager = manager;
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer,
                            InstrumentationContext context, OptimizedClassMatcher.Match match)
            throws IllegalClassFormatException {
      for (Method method : match.getMethods()) {
        for (ClassAndMethodMatcher matcher : match.getClassMatches().keySet()) {
          if (matcher.getMethodMatcher().matches(-1, method.getName(), method.getDescriptor(),
                                                        match.getMethodAnnotations(method))) {
            context.putTraceAnnotation(method, TraceDetailsBuilder.newBuilder()
                                                       .setMetricPrefix(matchersPrefix.get(matcher))
                                                       .build());
          }
        }
      }
      return null;
    }

    public boolean addTraceMatcher(ClassAndMethodMatcher matcher, String metricPrefix) {
      return !matchersPrefix.containsKey(matcher) && addMatchVisitor(matcher, metricPrefix);
    }

    private synchronized boolean addMatchVisitor(ClassAndMethodMatcher matcher, String metricPrefix) {
      if (!matchersPrefix.containsKey(matcher)) {
        matchersPrefix.put(matcher, metricPrefix);

        OptimizedClassMatcherBuilder builder = OptimizedClassMatcherBuilder.newBuilder();
        builder.addClassMethodMatcher(new ClassAndMethodMatcher[] {matcher});
        ClassMatchVisitorFactory matchVisitor = builder.build();
        matchVisitors.add(matchVisitor);
        contextManager.addContextClassTransformer(matchVisitor, this);

        return true;
      }
      return false;
    }
  }
}