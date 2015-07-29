package com.newrelic.agent.instrumentation.context;

import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.PointCut;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher.Match;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.agent.util.asm.BenignClassReadException;
import com.newrelic.agent.util.asm.ClassResolver;
import com.newrelic.agent.util.asm.ClassResolvers;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.deps.com.google.common.base.Supplier;
import com.newrelic.deps.com.google.common.collect.ImmutableMap;
import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.com.google.common.collect.Multimap;
import com.newrelic.deps.com.google.common.collect.Multimaps;
import com.newrelic.deps.com.google.common.collect.Sets;
import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

public class InstrumentationContext implements TraceDetailsList {
    private static final TraceInformation EMPTY_TRACE_INFO = new TraceInformation();
    protected final byte[] bytes;
    private final Class<?> classBeingRedefined;
    private final ProtectionDomain protectionDomain;
    protected boolean print;
    private boolean modified;
    private Multimap<Method, String> weavedMethods;
    private Set<Method> timedMethods;
    private Map<Method, PointCut> oldReflectionStyleInstrumentationMethods;
    private Map<Method, PointCut> oldInvokerStyleInstrumentationMethods;
    private TraceInformation tracedInfo;
    private Map<ClassMatchVisitorFactory, Match> matches;
    private Map<Method, Method> bridgeMethods;
    private List<ClassResolver> classResolvers;
    private boolean generated;
    private boolean hasSource;

    public InstrumentationContext(byte[] bytes, Class<?> classBeingRedefined, ProtectionDomain protectionDomain) {
        this.bytes = bytes;
        this.classBeingRedefined = classBeingRedefined;
        this.protectionDomain = protectionDomain;
    }

    public static Set<Class<?>> getMatchingClasses(final Collection<ClassMatchVisitorFactory> matchers,
                                                   Class<?>[] classes) {
        final Set matchingClasses = Sets.newConcurrentHashSet();
        if ((classes == null) || (classes.length == 0)) {
            return matchingClasses;
        }

        double partitions = classes.length < 8 ? classes.length : 8.0D;
        int estimatedPerPartition = (int) Math.ceil(classes.length / partitions);
        List<List<Class<?>>> partitionsClasses = Lists.partition(Arrays.asList(classes), estimatedPerPartition);

        final CountDownLatch countDownLatch = new CountDownLatch(partitionsClasses.size());
        for (final List<Class<?>> partitionClasses : partitionsClasses) {
            Runnable matchingRunnable = new Runnable() {
                public void run() {
                    for (Class clazz : partitionClasses) {
                        if (InstrumentationContext.isMatch(matchers, clazz)) {
                            matchingClasses.add(clazz);
                        }
                    }
                    countDownLatch.countDown();
                }
            };
            new Thread(matchingRunnable).start();
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Agent.LOG.log(Level.INFO, "Failed to wait for matching classes");
            Agent.LOG.log(Level.FINER, e, "Interrupted during class matching", new Object[0]);
        }

        return matchingClasses;
    }

    private static boolean isMatch(Collection<ClassMatchVisitorFactory> matchers, Class<?> clazz) {
        if (clazz.isArray()) {
            return false;
        }
        if ((clazz.getName().startsWith("com.newrelic.api.agent")) || (clazz.getName()
                                                                               .startsWith("com.newrelic.agent"
                                                                                                   + ".bridge"))) {
            return false;
        }
        ClassLoader loader = clazz.getClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        InstrumentationContext context = new InstrumentationContext(null, null, null);
        try {
            ClassReader reader = Utils.readClass(clazz);
            context.match(loader, clazz, reader, matchers);
            return !context.getMatches().isEmpty();
        } catch (BenignClassReadException ex) {
            return false;
        } catch (Exception ex) {
            if ((clazz.getName().startsWith("com.newrelic")) || (clazz.getName().startsWith("weave."))) {
                return false;
            }
            Agent.LOG.log(Level.FINER, "Unable to read {0}", new Object[] {clazz.getName()});
            Agent.LOG.log(Level.FINEST, ex, "Unable to read {0}", new Object[] {clazz.getName()});
        }
        return false;
    }

    public Class<?> getClassBeingRedefined() {
        return this.classBeingRedefined;
    }

    public ProtectionDomain getProtectionDomain() {
        return this.protectionDomain;
    }

    public void markAsModified() {
        this.modified = true;
    }

    public boolean isModified() {
        return this.modified;
    }

    public TraceInformation getTraceInformation() {
        return this.tracedInfo == null ? EMPTY_TRACE_INFO : this.tracedInfo;
    }

    public boolean isTracerMatch() {
        return (this.tracedInfo != null) && (this.tracedInfo.isMatch());
    }

    public void addWeavedMethod(Method method, String instrumentationTitle) {
        if (this.weavedMethods == null) {
            this.weavedMethods = Multimaps.newSetMultimap(Maps.<Method, Collection<String>>newHashMap(),
                                                                 new Supplier<Set<String>>() {
                                                                     public Set<String> get() {
                                                                         return Sets.newHashSet();
                                                                     }
                                                                 });
        }
        this.weavedMethods.put(method, instrumentationTitle);
        this.modified = true;
    }

    public void printBytecode() {
        this.print = true;
    }

    public PointCut getOldStylePointCut(Method method) {
        PointCut pc = (PointCut) getOldInvokerStyleInstrumentationMethods().get(method);
        if (null == pc) {
            pc = (PointCut) getOldReflectionStyleInstrumentationMethods().get(method);
        }
        return pc;
    }

    private Map<Method, PointCut> getOldInvokerStyleInstrumentationMethods() {
        return this.oldInvokerStyleInstrumentationMethods == null ? Collections.<Method, PointCut>emptyMap()
                       : this.oldInvokerStyleInstrumentationMethods;
    }

    private Map<Method, PointCut> getOldReflectionStyleInstrumentationMethods() {
        return this.oldReflectionStyleInstrumentationMethods == null ? Collections.<Method, PointCut>emptyMap()
                       : this.oldReflectionStyleInstrumentationMethods;
    }

    public Set<Method> getWeavedMethods() {
        return this.weavedMethods == null ? Collections.<Method>emptySet() : this.weavedMethods.keySet();
    }

    public Set<Method> getTimedMethods() {
        return this.timedMethods == null ? Collections.<Method>emptySet() : this.timedMethods;
    }

    public Collection<String> getMergeInstrumentationPackages(Method method) {
        return this.weavedMethods == null ? Collections.emptySet()
                       : (Collection) this.weavedMethods.asMap().get(method);
    }

    public boolean isModified(Method method) {
        return (getTimedMethods().contains(method)) || (getWeavedMethods().contains(method));
    }

    public void addTimedMethods(Method... methods) {
        if (this.timedMethods == null) {
            this.timedMethods = Sets.newHashSet();
        }
        Collections.addAll(this.timedMethods, methods);
        this.modified = true;
    }

    public void addOldReflectionStyleInstrumentationMethod(Method method, PointCut pointCut) {
        if (this.oldReflectionStyleInstrumentationMethods == null) {
            this.oldReflectionStyleInstrumentationMethods = Maps.newHashMap();
        }
        this.oldReflectionStyleInstrumentationMethods.put(method, pointCut);
        this.modified = true;
    }

    public void addOldInvokerStyleInstrumentationMethod(Method method, PointCut pointCut) {
        if (this.oldInvokerStyleInstrumentationMethods == null) {
            this.oldInvokerStyleInstrumentationMethods = Maps.newHashMap();
        }
        this.oldInvokerStyleInstrumentationMethods.put(method, pointCut);
        this.modified = true;
    }

    public Map<ClassMatchVisitorFactory, Match> getMatches() {
        return this.matches == null ? Collections.<ClassMatchVisitorFactory, Match>emptyMap() : this.matches;
    }

    byte[] processTransformBytes(byte[] originalBytes, byte[] newBytes) {
        if (null != newBytes) {
            markAsModified();
            return newBytes;
        }
        return originalBytes;
    }

    public void putTraceAnnotation(Method method, TraceDetails traceDetails) {
        if (this.tracedInfo == null) {
            this.tracedInfo = new TraceInformation();
        }
        this.tracedInfo.putTraceAnnotation(method, traceDetails);
    }

    public void addIgnoreApdexMethod(String methodName, String methodDesc) {
        if (this.tracedInfo == null) {
            this.tracedInfo = new TraceInformation();
        }
        this.tracedInfo.addIgnoreApdexMethod(methodName, methodDesc);
    }

    public void addIgnoreTransactionMethod(String methodName, String methodDesc) {
        if (this.tracedInfo == null) {
            this.tracedInfo = new TraceInformation();
        }
        this.tracedInfo.addIgnoreTransactionMethod(methodName, methodDesc);
    }

    public void addIgnoreTransactionMethod(Method m) {
        if (this.tracedInfo == null) {
            this.tracedInfo = new TraceInformation();
        }
        this.tracedInfo.addIgnoreTransactionMethod(m);
    }

    public void putMatch(ClassMatchVisitorFactory matcher, Match match) {
        if (this.matches == null) {
            this.matches = Maps.newHashMap();
        }
        this.matches.put(matcher, match);
    }

    public void addTracedMethods(Map<Method, TraceDetails> tracedMethods) {
        if (this.tracedInfo == null) {
            this.tracedInfo = new TraceInformation();
        }
        this.tracedInfo.pullAll(tracedMethods);
    }

    public void addTrace(Method method, TraceDetails traceDetails) {
        if (this.tracedInfo == null) {
            this.tracedInfo = new TraceInformation();
        }
        this.tracedInfo.putTraceAnnotation(method, traceDetails);
    }

    public void match(ClassLoader loader, Class<?> classBeingRedefined, ClassReader reader,
                      Collection<ClassMatchVisitorFactory> classVisitorFactories) {
        ClassVisitor visitor = null;
        for (ClassMatchVisitorFactory factory : classVisitorFactories) {
            ClassVisitor nextVisitor = factory.newClassMatchVisitor(loader, classBeingRedefined, reader, visitor, this);
            if (nextVisitor != null) {
                visitor = nextVisitor;
            }
        }
        if (visitor != null) {
            reader.accept(visitor, 1);
            if (this.bridgeMethods != null) {
                resolveBridgeMethods(reader);
            } else {
                this.bridgeMethods = ImmutableMap.of();
            }
        }
    }

    private void resolveBridgeMethods(ClassReader reader) {
        ClassVisitor visitor = new ClassVisitor(327680) {
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                             String[] exceptions) {
                final Method method = new Method(name, desc);
                if (InstrumentationContext.this.bridgeMethods.containsKey(method)) {
                    return new MethodVisitor(327680) {
                        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                            InstrumentationContext.this.bridgeMethods.put(method, new Method(name, desc));
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                        }

                    };
                }

                return null;
            }
        };
        reader.accept(visitor, 6);
    }

    public void addBridgeMethod(Method method) {
        if (this.bridgeMethods == null) {
            this.bridgeMethods = Maps.newHashMap();
        }
        this.bridgeMethods.put(method, method);
    }

    public Map<Method, Method> getBridgeMethods() {
        return this.bridgeMethods;
    }

    public boolean isUsingLegacyInstrumentation() {
        return (null != this.oldInvokerStyleInstrumentationMethods) || (null
                                                                                != this.oldReflectionStyleInstrumentationMethods);
    }

    public boolean hasModifiedClassStructure() {
        return null != this.oldInvokerStyleInstrumentationMethods;
    }

    public void addClassResolver(ClassResolver classResolver) {
        if (this.classResolvers == null) {
            this.classResolvers = Lists.newArrayList();
        }
        this.classResolvers.add(classResolver);
    }

    public ClassResolver getClassResolver(ClassLoader loader) {
        ClassResolver classResolver = ClassResolvers.getClassLoaderResolver(loader);
        if (this.classResolvers != null) {
            this.classResolvers.add(classResolver);
            classResolver = ClassResolvers.getMultiResolver(this.classResolvers);
        }
        return classResolver;
    }

    public byte[] getOriginalClassBytes() {
        return this.bytes;
    }

    public boolean isGenerated() {
        return this.generated;
    }

    public void setGenerated(boolean isGenerated) {
        this.generated = isGenerated;
    }

    public void setSourceAttribute(boolean hasSource) {
        this.hasSource = hasSource;
    }

    public boolean hasSourceAttribute() {
        return this.hasSource;
    }
}