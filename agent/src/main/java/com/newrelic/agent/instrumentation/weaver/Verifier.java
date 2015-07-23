//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.weaver;

import com.google.common.base.Predicate;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.agent.util.ClassUtils;
import com.newrelic.agent.util.asm.ClassStructure;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.weaver.Weave;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.FieldNode;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class Verifier {
    private static final int CLASS_STRUCTURE_FLAGS = 7;
    private final Cache<ClassLoader, Boolean> classLoaders;
    private final Map<Type, ClassStructure> resolvedClasses;
    private final LoadingCache<ClassLoader, AtomicInteger> classLoaderLocks;
    private final InstrumentationPackage instrumentationPackage;
    private final ClassStructureResolver classStructureResolver;
    private volatile Map<String, Set<MethodWithAccess>> referencedClassMethods;
    private volatile Map<String, Set<MethodWithAccess>> referencedInterfaceMethods;

    public Verifier(InstrumentationPackage instrumentationPackage) throws NoSuchMethodException, SecurityException {
        this(instrumentationPackage, new AgentClassStructureResolver());
    }

    public Verifier(InstrumentationPackage instrumentationPackage, ClassStructureResolver classStructureResolver)
            throws NoSuchMethodException, SecurityException {
        this.instrumentationPackage = instrumentationPackage;
        this.classStructureResolver = classStructureResolver;
        this.referencedClassMethods = Collections.emptyMap();
        this.referencedInterfaceMethods = Collections.emptyMap();
        this.classLoaders = CacheBuilder.newBuilder().weakKeys().expireAfterAccess(5L, TimeUnit.MINUTES).build();
        this.classLoaderLocks = CacheBuilder.newBuilder().weakKeys().expireAfterAccess(1L, TimeUnit.MINUTES)
                .build(new CacheLoader<ClassLoader, AtomicInteger>() {
                    public AtomicInteger load(ClassLoader key) throws Exception {
                        return new AtomicInteger();
                    }
                });
        this.resolvedClasses = Maps.newConcurrentMap();
    }

    private static void resolve(IAgentLogger logger, ClassStructureResolver classStructureResolver,
                                Map<String, Set<MethodWithAccess>> allReferenced, ClassLoader loader,
                                Map<String, ClassStructure> resolvedClasses, Set<String> unresolvedClasses,
                                boolean isInterface) {
        Iterator i$ = allReferenced.entrySet().iterator();

        while (i$.hasNext()) {
            Entry entry = (Entry) i$.next();
            String internalName = (String) entry.getKey();
            ClassStructure classStructure = null;

            try {
                classStructure = classStructureResolver.getClassStructure(logger, loader, internalName, 7);
            } catch (IOException var12) {
                logger.log(Level.FINEST, var12, "Error fetching class structure of {0} : {1}",
                        new Object[]{internalName, var12.getMessage()});
            }

            if (classStructure == null) {
                unresolvedClasses.add(internalName);
            } else {
                if (!((Set) entry.getValue()).isEmpty() && isInterface(classStructure.getAccess()) != isInterface) {
                    unresolvedClasses.add(internalName);
                    logger.finer(internalName + " is referenced as a" + (isInterface ? "n interface" : " class"));
                } else {
                    resolvedClasses.put(internalName, classStructure);
                }
            }
        }

    }

    private static boolean isInterface(int access) {
        return (access & 512) != 0;
    }

    public Map<Type, ClassStructure> getResolvedClasses() {
        return Collections.unmodifiableMap(this.resolvedClasses);
    }

    public String getImplementationTitle() {
        return this.instrumentationPackage.getImplementationTitle();
    }

    public boolean isEnabled(ClassLoader loader) {
        Boolean enabled = (Boolean) this.classLoaders.getIfPresent(loader);
        return enabled == null || enabled.booleanValue();
    }

    public boolean isVerified(ClassLoader loader) {
        Boolean verified = this.isVerifiedObject(loader);
        return verified != null && verified.booleanValue();
    }

    private Boolean isVerifiedObject(ClassLoader loader) {
        return (Boolean) this.classLoaders.getIfPresent(loader);
    }

    public boolean verify(ClassAppender classAppender, ClassLoader loader, Map<String, byte[]> classesInNewJar,
                          List<String> newClassLoadOrder) {
        Boolean verified = this.isVerifiedObject(loader);
        if (verified != null) {
            return verified.booleanValue();
        } else {
            verified = this.doVerify(classAppender, loader, classesInNewJar, newClassLoadOrder);
            if (verified == null) {
                return this.isVerified(loader);
            } else {
                this.classLoaders.put(loader, verified);
                if (verified.booleanValue()) {
                    this.instrumentationPackage.getLogger()
                            .debug("Loading " + this.getImplementationTitle() + " instrumentation");
                }

                StatsService statsService = ServiceFactory.getStatsService();
                statsService.doStatsWork(StatsWorks.getRecordMetricWork(MessageFormat.format(verified.booleanValue()
                                        ?
                                        "Supportability/WeaveInstrumentation/Loaded/{0}/{1}"
                                        : "Supportability/WeaveInstrumentation/Skipped/{0}/{1}",
                                new Object[]
                                        {
                                                this.getImplementationTitle(),
                                                Float.valueOf(this.instrumentationPackage
                                                        .getImplementationVersion())
                                        }),
                        1.0F));
                return verified.booleanValue();
            }
        }
    }

    private Boolean doVerify(ClassAppender classAppender, ClassLoader loader, Map<String, byte[]> classesInNewJar,
                             List<String> newClassLoadOrder) {
        HashMap resolvedClasses = Maps.newHashMap();
        HashSet unresolvedClasses = Sets.newHashSet();
        this.resolveWeaveClasses(loader, unresolvedClasses);
        if (!unresolvedClasses.isEmpty()) {
            this.instrumentationPackage.getLogger()
                    .finer("Skipping " + this.getImplementationTitle() + " instrumentation.  Unresolved classes: "
                            + unresolvedClasses);
            return Boolean.valueOf(false);
        } else {
            if (!this.instrumentationPackage.getSkipClasses().isEmpty()) {
                this.instrumentationPackage.getLogger()
                        .finest("Checking for the presence of classes: " + this.instrumentationPackage
                                .getSkipClasses());
            }

            if (this.shouldSkip(loader)) {
                return Boolean.valueOf(false);
            } else {
                resolve(this.instrumentationPackage.getLogger(), this.classStructureResolver,
                        this.referencedClassMethods, loader, resolvedClasses, unresolvedClasses, false);
                resolve(this.instrumentationPackage.getLogger(), this.classStructureResolver,
                        this.referencedInterfaceMethods, loader, resolvedClasses, unresolvedClasses, true);
                unresolvedClasses.removeAll(resolvedClasses.keySet());
                HashMap allReferenced = Maps.newHashMap(this.referencedClassMethods);
                allReferenced.putAll(this.referencedInterfaceMethods);
                HashSet set = Sets.newHashSet(unresolvedClasses);
                set.removeAll(classesInNewJar.keySet());
                if (!set.isEmpty()) {
                    this.instrumentationPackage.getLogger().finer("Skipping " + this.getImplementationTitle()
                            + " instrumentation.  Unresolved classes: "
                            + set);
                    return Boolean.valueOf(false);
                } else {
                    Iterator copy = resolvedClasses.entrySet().iterator();

                    while (copy.hasNext()) {
                        Entry ex = (Entry) copy.next();

                        try {
                            Set e = (Set) allReferenced.get(ex.getKey());
                            if (!e.isEmpty()) {
                                this.verifyMethods(loader, e, (ClassStructure) ex.getValue());
                                if (!e.isEmpty()) {
                                    this.instrumentationPackage.getLogger()
                                            .finer("Skipping " + this.getImplementationTitle() + " instrumentation.  "
                                                    + (String) ex.getKey() + " unresolved methods: " + e);
                                    return Boolean.valueOf(false);
                                }
                            }
                        } catch (IOException var14) {
                            this.instrumentationPackage.getLogger().log(Level.FINER, "Verifier error", var14);
                        }
                    }

                    HashMap copy1 = Maps.newHashMap(classesInNewJar);
                    copy1.keySet().retainAll(unresolvedClasses);
                    if (!copy1.isEmpty()) {
                        try {
                            AtomicInteger ex1 = (AtomicInteger) this.classLoaderLocks.get(loader);
                            if (ex1.getAndIncrement() == 0) {
                                try {
                                    this.loadClasses(classAppender, loader, copy1, newClassLoadOrder);
                                } catch (Exception var12) {
                                    this.instrumentationPackage.getLogger()
                                            .log(Level.FINEST, "Error loading unresolved clases: " + copy1, var12);
                                    return null;
                                }
                            }
                        } catch (ExecutionException var13) {
                            Agent.LOG.log(Level.FINEST, var13, var13.toString(), new Object[0]);
                            return this.isVerifiedObject(loader);
                        }
                    }

                    return Boolean.valueOf(true);
                }
            }
        }
    }

    private boolean shouldSkip(ClassLoader loader) {
        Iterator i$ = this.instrumentationPackage.getSkipClasses().iterator();

        while (i$.hasNext()) {
            String className = (String) i$.next();
            ClassStructure classStructure = null;

            try {
                classStructure = this.getClassStructure(this.instrumentationPackage.getLogger(), loader, className);
            } catch (IOException var6) {
                ;
            }

            if (classStructure != null) {
                this.instrumentationPackage.getLogger()
                        .log(Level.FINER, "Skipping weave package because {0} is present", new Object[]{className});
                return true;
            }
        }

        return false;
    }

    private void resolveWeaveClasses(ClassLoader loader, Set<String> unresolvedClasses) {
        Iterator i$ = this.instrumentationPackage.getWeaveClasses().entrySet().iterator();

        while (i$.hasNext()) {
            Entry entry = (Entry) i$.next();
            String internalName = (String) entry.getKey();
            ClassStructure classStructure = null;

            try {
                classStructure = this.getClassStructure(this.instrumentationPackage.getLogger(), loader, internalName);
            } catch (IOException var11) {
                this.instrumentationPackage.getLogger()
                        .log(Level.WARNING, "Could not resolved class structure for {0}", new Object[]{internalName});
            }

            if (classStructure != null && !classStructure.getClassAnnotations()
                    .containsKey(Type.getDescriptor(Weave.class))) {
                Collection referencedFields = ((WeavedClassInfo) entry.getValue()).getReferencedFields();
                Iterator i$1 = referencedFields.iterator();

                while (i$1.hasNext()) {
                    FieldNode field = (FieldNode) i$1.next();
                    FieldNode fieldNode = (FieldNode) classStructure.getFields().get(field.name);
                    if (fieldNode == null) {
                        unresolvedClasses.add(internalName);
                        this.instrumentationPackage.getLogger()
                                .finer("Field " + field.name + " does not exist on " + internalName);
                    } else {
                        if (!fieldNode.desc.equals(field.desc)) {
                            this.instrumentationPackage.getLogger()
                                    .finer("Expected field " + field.name + " on " + internalName
                                            + " to have the signature " + field.desc + ", but found "
                                            + fieldNode.desc);
                        }
                    }
                }
            } else {
                unresolvedClasses.add(internalName);
            }
        }

    }

    private ClassStructure getClassStructure(Logger logger, ClassLoader loader, String internalName)
            throws IOException {
        return this.classStructureResolver.getClassStructure(logger, loader, internalName, 7);
    }

    private void verifyMethods(ClassLoader loader, Set<MethodWithAccess> methods, ClassStructure classStructure)
            throws IOException {
        this.resolvedClasses.put(classStructure.getType(), classStructure);
        Set classStructureMethods = classStructure.getMethods();
        HashSet methodsInClassStructure = Sets.newHashSet();
        Iterator superName = methods.iterator();

        while (superName.hasNext()) {
            MethodWithAccess len$ = (MethodWithAccess) superName.next();
            Method i$ = len$.getMethod();
            if (classStructureMethods.contains(i$) && classStructure.isStatic(i$).booleanValue() == len$.isStatic()) {
                methodsInClassStructure.add(len$);
            }
        }

        methods.removeAll(methodsInClassStructure);
        if (!methods.isEmpty()) {
            String[] var10 = classStructure.getInterfaces();
            int var12 = var10.length;

            for (int var13 = 0; var13 < var12; ++var13) {
                String interfaceClass = var10[var13];
                this.verifyMethods(loader, methods,
                        this.getClassStructure(this.instrumentationPackage.getLogger(), loader,
                                interfaceClass));
                if (methods.isEmpty()) {
                    return;
                }
            }

            String var11 = classStructure.getSuperName();
            if (var11 != null) {
                this.verifyMethods(loader, methods,
                        this.getClassStructure(this.instrumentationPackage.getLogger(), loader,
                                var11));
            }

        }
    }

    private void loadClasses(ClassAppender classAppender, ClassLoader loader, Map<String, byte[]> classBytes,
                             List<String> newClassLoadOrder) throws IOException {
        ArrayList loadedClasses = Lists.newArrayList();
        Iterator i$ = classBytes.entrySet().iterator();

        while (i$.hasNext()) {
            Entry nameAndBytes = (Entry) i$.next();

            try {
                Class ex = loader.loadClass(Type.getObjectType((String) nameAndBytes.getKey()).getClassName());
                if (ex.getClassLoader() == null || ex.getClassLoader().equals(loader) || this.isFullyResolveable(loader,
                        ex,
                        (byte[]) nameAndBytes
                                .getValue(),
                        classBytes
                                .keySet())) {
                    loadedClasses.add(Type.getInternalName(ex));
                }
            } catch (Exception var9) {
                ;
            }
        }

        if (!loadedClasses.isEmpty()) {
            this.instrumentationPackage.getLogger()
                    .finer(this.getImplementationTitle() + " skipping already loaded classes: " + loadedClasses);
            classBytes.keySet().removeAll(loadedClasses);
        }

        if (!classBytes.isEmpty()) {
            this.instrumentationPackage.getLogger()
                    .finer(this.getImplementationTitle() + " loading classes: " + classBytes.keySet()
                            + " using class loader " + loader);
            classAppender.appendClasses(loader, classBytes, newClassLoadOrder);
        }

    }

    private boolean isFullyResolveable(ClassLoader loader, Class<?> clazz, byte[] classBytes,
                                       Set<String> newClassNames) {
        Set referencedClasses = ClassUtils.getClassReferences(classBytes);
        referencedClasses.removeAll(newClassNames);
        referencedClasses = Sets.filter(referencedClasses, new Predicate<String>() {
            public boolean apply(String internalClassName) {
                return !internalClassName.startsWith("java/");
            }
        });
        Iterator i$ = referencedClasses.iterator();

        while (i$.hasNext()) {
            String internalClassName = (String) i$.next();

            try {
                String e = Type.getObjectType(internalClassName).getClassName();
                Class throughLoader = loader.loadClass(e);
                Class throughClassLoader = clazz.getClassLoader().loadClass(e);
                if (throughLoader != throughClassLoader && (!throughLoader.isAssignableFrom(throughClassLoader)
                        || !throughClassLoader
                        .isAssignableFrom(throughLoader))) {
                    this.instrumentationPackage.getLogger().log(Level.FINEST,
                            "{0} was resolved through class loader {1}, "
                                    + "but it references {2} and the "
                                    + "version of that class loaded "
                                    + "through {3} differs from the one "
                                    + "loaded through {4}",
                            new Object[]{
                                    clazz.getName(),
                                    clazz.getClassLoader(), e,
                                    loader, throughClassLoader
                                    .getClassLoader()
                            });
                    return false;
                }
            } catch (ClassNotFoundException var11) {
                ;
            }
        }

        return true;
    }

    public ClassStructure getClassStructure(Type type) {
        ClassStructure classStructure = (ClassStructure) this.getResolvedClasses().get(type);
        if (classStructure == null) {
            Iterator i$ = this.classLoaders.asMap().entrySet().iterator();

            while (i$.hasNext()) {
                Entry entry = (Entry) i$.next();
                if (((Boolean) entry.getValue()).booleanValue()) {
                    URL resource = ((ClassLoader) entry.getKey())
                            .getResource(Utils.getClassResourceName(type.getInternalName()));
                    if (resource != null) {
                        try {
                            classStructure = ClassStructure.getClassStructure(resource);
                        } catch (IOException var7) {
                            this.instrumentationPackage.getLogger()
                                    .finest("Unable to load structure of " + type.getClassName());
                        }
                    }
                }
            }
        }

        return classStructure;
    }

    void setReferences(Map<String, Set<MethodWithAccess>> referencedClassMethods,
                       Map<String, Set<MethodWithAccess>> referencedInterfaceMethods) {
        this.referencedClassMethods = Collections.unmodifiableMap(referencedClassMethods);
        this.referencedInterfaceMethods = Collections.unmodifiableMap(referencedInterfaceMethods);
    }
}
