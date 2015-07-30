//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.weaver;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

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
import com.newrelic.deps.com.google.common.base.Predicate;
import com.newrelic.deps.com.google.common.cache.Cache;
import com.newrelic.deps.com.google.common.cache.CacheBuilder;
import com.newrelic.deps.com.google.common.cache.CacheLoader;
import com.newrelic.deps.com.google.common.cache.LoadingCache;
import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.com.google.common.collect.Sets;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.Method;
import com.newrelic.deps.org.objectweb.asm.tree.FieldNode;

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

        for (Entry<String, Set<MethodWithAccess>> entry : allReferenced.entrySet()) {
            String internalName = entry.getKey();
            ClassStructure classStructure = null;

            try {
                classStructure = classStructureResolver.getClassStructure(logger, loader, internalName, 7);
            } catch (IOException var12) {
                logger.log(Level.FINEST, var12, "Error fetching class structure of {0} : {1}", internalName,
                                  var12.getMessage());
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
        Boolean enabled = this.classLoaders.getIfPresent(loader);
        return enabled == null || enabled;
    }

    public boolean isVerified(ClassLoader loader) {
        Boolean verified = this.isVerifiedObject(loader);
        return verified != null && verified;
    }

    private Boolean isVerifiedObject(ClassLoader loader) {
        return this.classLoaders.getIfPresent(loader);
    }

    public boolean verify(ClassAppender classAppender, ClassLoader loader, Map<String, byte[]> classesInNewJar,
                          List<String> newClassLoadOrder) {
        Boolean verified = this.isVerifiedObject(loader);
        if (verified != null) {
            return verified;
        } else {
            verified = this.doVerify(classAppender, loader, classesInNewJar, newClassLoadOrder);
            if (verified == null) {
                return this.isVerified(loader);
            } else {
                this.classLoaders.put(loader, verified);
                if (verified) {
                    this.instrumentationPackage.getLogger()
                            .debug("Loading " + this.getImplementationTitle() + " instrumentation");
                }

                StatsService statsService = ServiceFactory.getStatsService();
                statsService.doStatsWork(StatsWorks.getRecordMetricWork(MessageFormat.format(verified
                                                                                                     ?
                                                                                                     "Supportability/WeaveInstrumentation/Loaded/{0}/{1}"
                                                                                                     : "Supportability/WeaveInstrumentation/Skipped/{0}/{1}",

                                                                                                    this.getImplementationTitle(),

                                                                                                    this.instrumentationPackage
                                                                                                            .getImplementationVersion()),
                                                                               1.0F));
                return verified;
            }
        }
    }

    private Boolean doVerify(ClassAppender classAppender, ClassLoader loader, Map<String, byte[]> classesInNewJar,
                             List<String> newClassLoadOrder) {
        HashMap<String, ClassStructure> resolvedClasses = Maps.newHashMap();
        HashSet<String> unresolvedClasses = Sets.newHashSet();
        this.resolveWeaveClasses(loader, unresolvedClasses);
        if (!unresolvedClasses.isEmpty()) {
            this.instrumentationPackage.getLogger()
                    .finer("Skipping " + this.getImplementationTitle() + " instrumentation.  Unresolved classes: "
                                   + unresolvedClasses);
            return false;
        } else {
            if (!this.instrumentationPackage.getSkipClasses().isEmpty()) {
                this.instrumentationPackage.getLogger()
                        .finest("Checking for the presence of classes: " + this.instrumentationPackage
                                                                                   .getSkipClasses());
            }

            if (this.shouldSkip(loader)) {
                return false;
            } else {
                resolve(this.instrumentationPackage.getLogger(), this.classStructureResolver,
                               this.referencedClassMethods, loader, resolvedClasses, unresolvedClasses, false);
                resolve(this.instrumentationPackage.getLogger(), this.classStructureResolver,
                               this.referencedInterfaceMethods, loader, resolvedClasses, unresolvedClasses, true);
                unresolvedClasses.removeAll(resolvedClasses.keySet());
                HashMap<String, Set<MethodWithAccess>> allReferenced = Maps.newHashMap(this.referencedClassMethods);
                allReferenced.putAll(this.referencedInterfaceMethods);
                HashSet<String> set = Sets.newHashSet(unresolvedClasses);
                set.removeAll(classesInNewJar.keySet());
                if (!set.isEmpty()) {
                    this.instrumentationPackage.getLogger().finer("Skipping " + this.getImplementationTitle()
                                                                          + " instrumentation.  Unresolved classes: "
                                                                          + set);
                    return false;
                } else {
                    for (Entry<String, ClassStructure> ex : resolvedClasses.entrySet()) {
                        try {
                            Set e = (Set) allReferenced.get(ex.getKey());
                            if (!e.isEmpty()) {
                                this.verifyMethods(loader, e, ex.getValue());
                                if (!e.isEmpty()) {
                                    this.instrumentationPackage.getLogger()
                                            .finer("Skipping " + this.getImplementationTitle() + " instrumentation.  "
                                                           + ex.getKey() + " unresolved methods: " + e);
                                    return false;
                                }
                            }
                        } catch (IOException var14) {
                            this.instrumentationPackage.getLogger().log(Level.FINER, "Verifier error", var14);
                        }
                    }

                    HashMap<String, byte[]> copy1 = Maps.newHashMap(classesInNewJar);
                    copy1.keySet().retainAll(unresolvedClasses);
                    if (!copy1.isEmpty()) {
                        try {
                            AtomicInteger ex1 = this.classLoaderLocks.get(loader);
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
                            Agent.LOG.log(Level.FINEST, var13, var13.toString());
                            return this.isVerifiedObject(loader);
                        }
                    }

                    return true;
                }
            }
        }
    }

    private boolean shouldSkip(ClassLoader loader) {
        for (String className : this.instrumentationPackage.getSkipClasses()) {
            ClassStructure classStructure = null;

            try {
                classStructure = this.getClassStructure(this.instrumentationPackage.getLogger(), loader, className);
            } catch (IOException var6) {
                ;
            }

            if (classStructure != null) {
                this.instrumentationPackage.getLogger()
                        .log(Level.FINER, "Skipping weave package because {0} is present", className);
                return true;
            }
        }

        return false;
    }

    private void resolveWeaveClasses(ClassLoader loader, Set<String> unresolvedClasses) {
        for (Entry<String, WeavedClassInfo> entry : this.instrumentationPackage.getWeaveClasses().entrySet()) {
            String internalName = entry.getKey();
            ClassStructure classStructure = null;

            try {
                classStructure = this.getClassStructure(this.instrumentationPackage.getLogger(), loader, internalName);
            } catch (IOException var11) {
                this.instrumentationPackage.getLogger()
                        .log(Level.WARNING, "Could not resolved class structure for {0}", internalName);
            }

            if (classStructure != null && !classStructure.getClassAnnotations()
                                                   .containsKey(Type.getDescriptor(Weave.class))) {
                Collection<FieldNode> referencedFields = entry.getValue().getReferencedFields();

                for (FieldNode field : referencedFields) {
                    FieldNode fieldNode = classStructure.getFields().get(field.name);
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
        HashSet<MethodWithAccess> methodsInClassStructure = Sets.newHashSet();

        for (MethodWithAccess access : methods) {
            Method method = access.getMethod();
            if (classStructureMethods.contains(method) && classStructure.isStatic(method) == access.isStatic()) {
                methodsInClassStructure.add(access);
            }
        }

        methods.removeAll(methodsInClassStructure);
        if (!methods.isEmpty()) {
            String[] interfaces = classStructure.getInterfaces();

            for (String interfaceClass : interfaces) {
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
        ArrayList<String> loadedClasses = Lists.newArrayList();

        for (Entry<String, byte[]> nameAndBytes : classBytes.entrySet()) {
            try {
                Class ex = loader.loadClass(Type.getObjectType(nameAndBytes.getKey()).getClassName());
                if (ex.getClassLoader() == null || ex.getClassLoader().equals(loader) || this.isFullyResolveable(loader,
                                                                                                                        ex,
                                                                                                                        nameAndBytes
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
        Set<String> referencedClasses = ClassUtils.getClassReferences(classBytes);
        referencedClasses.removeAll(newClassNames);
        referencedClasses = Sets.filter(referencedClasses, new Predicate<String>() {
            public boolean apply(String internalClassName) {
                return !internalClassName.startsWith("java/");
            }
        });

        for (String internalClassName : referencedClasses) {
            try {
                String e = Type.getObjectType(internalClassName).getClassName();
                Class<?> throughLoader = loader.loadClass(e);
                Class<?> throughClassLoader = clazz.getClassLoader().loadClass(e);
                if (throughLoader != throughClassLoader && (!throughLoader.isAssignableFrom(throughClassLoader)
                                                                    || !throughClassLoader
                                                                                .isAssignableFrom(throughLoader))) {
                    this.instrumentationPackage.getLogger().log(Level.FINEST,
                                                                       "{0} was resolved through class loader {1}, "
                                                                               + "but it references {2} and the "
                                                                               + "version of that class loaded "
                                                                               + "through {3} differs from the one "
                                                                               + "loaded through {4}", clazz.getName(),
                                                                       clazz.getClassLoader(), e, loader,
                                                                       throughClassLoader.getClassLoader());
                    return false;
                }
            } catch (ClassNotFoundException var11) {
                ;
            }
        }

        return true;
    }

    public ClassStructure getClassStructure(Type type) {
        ClassStructure classStructure = this.getResolvedClasses().get(type);
        if (classStructure == null) {

            for (Entry<ClassLoader, Boolean> entry : this.classLoaders.asMap().entrySet()) {

                if (entry.getValue()) {
                    URL resource = entry.getKey().getResource(Utils.getClassResourceName(type.getInternalName()));
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
