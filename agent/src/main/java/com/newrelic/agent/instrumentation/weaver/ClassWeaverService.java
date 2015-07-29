package com.newrelic.agent.instrumentation.weaver;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.regex.Pattern;

import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.Type;

import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.config.AgentJarHelper;
import com.newrelic.agent.extension.ExtensionService;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.AgentError;
import com.newrelic.agent.util.asm.BenignClassReadException;
import com.newrelic.bootstrap.BootstrapAgent;

public class ClassWeaverService {
    private static final int PARTITONS = 8;
    private final Map<String, InstrumentationPackage> instrumentationPackageNames = Maps.newConcurrentMap();
    private final InstrumentationContextManager contextManager;
    private final Set<InstrumentationPackage> instrumentationPackages = Sets.newCopyOnWriteArraySet();
    private final Set<InstrumentationPackage> internalInstrumentationPackages = Sets.newCopyOnWriteArraySet();

    public ClassWeaverService(InstrumentationContextManager contextManager) {
        this.contextManager = contextManager;
    }

    public InstrumentationContextManager getContextManager() {
        return contextManager;
    }

    public InstrumentationPackage getInstrumentationPackage(String implementationTitle) {
        return instrumentationPackageNames.get(implementationTitle);
    }

    public void removeInstrumentationPackage(InstrumentationPackage instrumentationPackage) {
        instrumentationPackageNames.remove(instrumentationPackage.getImplementationTitle());
        contextManager.removeMatchVisitor(instrumentationPackage.getMatcher());
    }

    public void addWeavingClassTransformer(WeavingClassTransformer transformer) {
        instrumentationPackageNames
                .put(transformer.instrumentationPackage.getImplementationTitle(), transformer.instrumentationPackage);

        contextManager.addContextClassTransformer(transformer.instrumentationPackage.getMatcher(), transformer);
    }

    public Runnable registerInstrumentation() {
        AgentBridge.objectFieldManager = new ObjectFieldManagerImpl();

        Collection<String> jarFileNames =
                AgentJarHelper.findAgentJarFileNames(Pattern.compile("instrumentation\\/(.*).jar"));
        if (jarFileNames.isEmpty()) {
            Agent.LOG.error("No instrumentation packages were found in the agent.");
        } else {
            Agent.LOG.fine("Loading " + jarFileNames.size() + " instrumentation packages.");
        }
        addInternalInstrumentationPackagesInParallel(jarFileNames);
        ExtensionService extensionService = ServiceFactory.getExtensionService();
        return reloadInstrumentationPackages(extensionService.getWeaveExtensions(), true);
    }

    private void addInternalInstrumentationPackagesInParallel(Collection<String> jarFileNames) {
        int partitions = jarFileNames.size() < PARTITONS ? jarFileNames.size() : PARTITONS;
        List<Set<String>> instrumentationPartitions = partitionInstrumentationJars(jarFileNames, partitions);

        final CountDownLatch executorCountDown = new CountDownLatch(partitions);
        for (final Set instrumentationJars : instrumentationPartitions) {
            Runnable instrumentationRunnable = new Runnable() {
                public void run() {
                    internalInstrumentationPackages.addAll(getInternalInstrumentationPackages(instrumentationJars));
                    executorCountDown.countDown();
                }
            };
            new Thread(instrumentationRunnable).start();
        }

        try {
            executorCountDown.await();
        } catch (InterruptedException e) {
            Agent.LOG.log(Level.FINE, "Interrupted while waiting for instrumentation packages.", e);
        }
    }

    private List<Set<String>> partitionInstrumentationJars(Collection<String> jarFileNames, int partitions) {
        List<Set<String>> instrumentationPartitions = new ArrayList<Set<String>>(partitions);

        for (int i = 0; i < partitions; i++) {
            instrumentationPartitions.add(new HashSet<String>());
        }

        int index = 0;
        for (String jarFileName : jarFileNames) {
            instrumentationPartitions.get(index++ % partitions).add(jarFileName);
        }

        return instrumentationPartitions;
    }

    public Runnable reloadInstrumentationPackages(Collection<File> weaveExtensions) {
        return reloadInstrumentationPackages(weaveExtensions, false);
    }

    private Runnable reloadInstrumentationPackages(Collection<File> weaveExtensions,
                                                   boolean retransformInternalInstrumentationPackageMatches) {
        Collection<ClassMatchVisitorFactory> unloadedMatchers = Sets.newHashSet();
        final Set<InstrumentationPackage> toClose = Sets.newHashSet(instrumentationPackages);

        for (InstrumentationPackage ip : instrumentationPackages) {
            unloadedMatchers.add(ip.getMatcher());
            removeInstrumentationPackage(ip);
        }
        Collection<ClassMatchVisitorFactory> loadedMatchers = Sets.newHashSet();
        instrumentationPackages.clear();
        instrumentationPackages.addAll(getInstrumentationPackages(weaveExtensions));
        loadedMatchers.addAll(buildTransformers(retransformInternalInstrumentationPackageMatches));

        if (!retransformInternalInstrumentationPackageMatches) {
            Collection<ClassDefinition> existingClasses = Lists.newLinkedList();
            for (InstrumentationPackage ip : instrumentationPackages) {
                for (Iterator iterator = toClose.iterator(); iterator.hasNext(); ) {
                    InstrumentationPackage oldIp = (InstrumentationPackage) iterator.next();
                    if ((oldIp.getCloseables().isEmpty()) || (oldIp.getImplementationTitle()
                                                                      .equals(ip.getImplementationTitle()))) {
                        iterator.remove();
                    }
                }
                for (Entry className : ip.newClasses.entrySet()) {
                    try {
                        Class existingClass =
                                getClass().getClassLoader().loadClass(((String) className.getKey()).replace("/", "."));

                        existingClasses.add(new ClassDefinition(existingClass, (byte[]) className.getValue()));
                    } catch (ClassNotFoundException e) {
                    }
                }
            }
            if ((!existingClasses.isEmpty()) && (ServiceFactory.getAgent().getInstrumentation()
                                                         .isRedefineClassesSupported())) {
                try {
                    ServiceFactory.getAgent().getInstrumentation()
                            .redefineClasses(existingClasses.toArray(new ClassDefinition[existingClasses.size()]));
                } catch (Exception e) {
                    if (!Agent.LOG.isFinestEnabled()) {
                        Agent.LOG.fine("Error redefining classes: " + e.getMessage());
                    } else {
                        Agent.LOG.log(Level.FINEST, "Error redefining classes", e);
                    }
                }
            }
        }

        final Collection matchers = Sets.newHashSet(loadedMatchers);
        matchers.addAll(unloadedMatchers);

        return new Runnable() {
            public void run() {
                ServiceFactory.getClassTransformerService().retransformMatchingClassesImmediately(matchers);
                InstrumentationPackage ip;
                for (InstrumentationPackage aToClose : toClose) {
                    ip = aToClose;
                    for (Closeable closeable : ip.getCloseables()) {
                        try {
                            closeable.close();
                        } catch (IOException e) {
                            Agent.LOG.log(Level.FINE, e, "Error closing InstrumentationPackage {0} closeable {1}",
                                                 ip.implementationTitle, closeable);
                        }
                    }
                }
            }
        };
    }

    private Collection<ClassMatchVisitorFactory> buildTransformers(boolean retransformInternalInstrumentationPackageMatches) {

        Set<InstrumentationPackage> filteredInstrumentationPackages =
                filter(instrumentationPackages, internalInstrumentationPackages);

        instrumentationPackages.retainAll(filteredInstrumentationPackages);
        if (!retransformInternalInstrumentationPackageMatches) {
            filteredInstrumentationPackages.retainAll(instrumentationPackages);
        }
        List<WeavingClassTransformer> transformers = createTransformers(filteredInstrumentationPackages);

        for (WeavingClassTransformer transformer : transformers) {
            addWeavingClassTransformer(transformer);

            transformer.instrumentationPackage.getLogger()
                    .debug("Registered " + transformer.instrumentationPackage.getImplementationTitle());
        }

        Collection<ClassMatchVisitorFactory> matchers = Sets.newHashSet();
        if (retransformInternalInstrumentationPackageMatches) {
            for (WeavingClassTransformer transformer : transformers) {
                matchers.add(transformer.instrumentationPackage.getMatcher());
            }
        } else {
            for (InstrumentationPackage instrumentationPackage : instrumentationPackages) {
                matchers.add(instrumentationPackage.getMatcher());
            }
        }
        return matchers;
    }

    private List<WeavingClassTransformer> createTransformers(Set<InstrumentationPackage> instrumentationPackages) {
        List<WeavingClassTransformer> transformers = Lists.newLinkedList();
        for (InstrumentationPackage instrumentationPackage : instrumentationPackages) {
            try {
                WeavingClassTransformer transformer =
                        getTransformer(instrumentationPackage, instrumentationPackage.getLocation());

                if (transformer != null) {
                    transformers.add(transformer);
                }
            } catch (Exception e) {
                instrumentationPackage.getLogger()
                        .severe("Unable to load " + instrumentationPackage.getLocation() + " : " + e.getMessage());

                instrumentationPackage.getLogger()
                        .log(Level.FINEST, "Unable to load instrumentation jar " + instrumentationPackage.getLocation(),
                                    e);
            }
        }

        return transformers;
    }

    private Set<InstrumentationPackage> getInstrumentationPackages(Collection<File> weaveExtensions) {
        Set<InstrumentationPackage> instrumentationPackages = Sets.newHashSet();
        for (File file : weaveExtensions) {
            if (!file.exists()) {
                Agent.LOG.error("Unable to find instrumentation jar: " + file.getAbsolutePath());
            } else {
                InputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(file);
                    addInstrumentationPackage(instrumentationPackages, inputStream, file.getAbsolutePath());
                } catch (IOException e) {
                    Agent.LOG.severe("Unable to open " + file.getAbsolutePath());
                } finally {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        return instrumentationPackages;
    }

    private Set<InstrumentationPackage> getInternalInstrumentationPackages(Collection<String> jarFileNames) {
        Set<InstrumentationPackage> instrumentationPackages = Sets.newHashSet();
        for (String name : jarFileNames) {
            URL instrumentationUrl = BootstrapAgent.class.getResource('/' + name);
            if (instrumentationUrl == null) {
                Agent.LOG.error("Unable to find instrumentation jar: " + name);
            } else {
                InputStream inputStream = null;
                try {
                    inputStream = instrumentationUrl.openStream();
                    addInstrumentationPackage(instrumentationPackages, inputStream, instrumentationUrl.getFile());
                } catch (IOException e) {
                    Agent.LOG.severe("Unable to open " + instrumentationUrl.getFile());
                } finally {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        return instrumentationPackages;
    }

    private void addInstrumentationPackage(Set<InstrumentationPackage> instrumentationPackages, InputStream inputStream,
                                           String location) {
        IAgentLogger logger = Agent.LOG;
        try {
            try {
                JarInputStream jarStream = new JarInputStream(inputStream);

                InstrumentationMetadata metadata = new InstrumentationMetadata(jarStream, location);
                logger = Agent.LOG.getChildLogger(metadata.getImplementationTitle());
                InstrumentationPackage instrumentationPackage =
                        new InstrumentationPackage(getContextManager().getInstrumentation(), logger, metadata,
                                                          jarStream);

                logger.debug("Loaded " + metadata.getImplementationTitle());
                if (instrumentationPackage.isEnabled()) {
                    instrumentationPackages.add(instrumentationPackage);
                }
            } finally {
                inputStream.close();
            }
        } catch (Exception e) {
            logger.severe("Unable to load " + location + " : " + e.getMessage());
            logger.log(Level.FINEST, "Unable to load instrumentation " + location, e);
        } catch (AgentError e) {
            logger.severe("Unable to load " + location + " : " + e.getMessage());
            logger.log(Level.FINEST, "Unable to load instrumentation " + location, e);
        }
    }

    private Set<InstrumentationPackage> filter(Set<InstrumentationPackage> instrumentationPackages,
                                               Set<InstrumentationPackage> internalInstrumentationPackages) {
        Set<InstrumentationPackage> filteredPackages = Sets.newHashSet();
        filteredPackages.addAll(instrumentationPackages);
        filteredPackages.addAll(internalInstrumentationPackages);

        Map filtered = Maps.newHashMap();
        for (InstrumentationPackage instrumentationPackage : filteredPackages) {
            InstrumentationPackage existing =
                    (InstrumentationPackage) filtered.get(instrumentationPackage.getImplementationTitle());
            if (existing != null) {
                if (existing.getImplementationVersion() == instrumentationPackage.getImplementationVersion()) {
                    Agent.LOG.severe(instrumentationPackage.getLocation() + " is named " + instrumentationPackage
                                                                                                   .getImplementationTitle()
                                             + " which conflicts with the title of " + existing.getLocation());
                } else if (existing.getImplementationVersion() > instrumentationPackage.getImplementationVersion()) {
                    Agent.LOG.debug(instrumentationPackage.getImplementationTitle() + " v" + instrumentationPackage
                                                                                                     .getImplementationVersion()
                                            + " in " + instrumentationPackage.getLocation() + " is older than version "
                                            + existing.getImplementationVersion() + " in " + existing.getLocation());
                } else {
                    filtered.put(instrumentationPackage.getImplementationTitle(), instrumentationPackage);
                }
            } else {
                filtered.put(instrumentationPackage.getImplementationTitle(), instrumentationPackage);
            }

        }

        filteredPackages.retainAll(filtered.values());
        return filteredPackages;
    }

    private WeavingClassTransformer getTransformer(InstrumentationPackage instrumentationPackage, String name) {
        boolean containsJDKClasses = instrumentationPackage.containsJDKClasses();
        return containsJDKClasses ? new BootstrapClassTransformer(instrumentationPackage)
                       : new WeavingClassTransformer(instrumentationPackage);
    }

    public void loadClass(ClassLoader classLoader, String implementationTitle, String className) throws IOException {
        InstrumentationPackage instrumentationPackage = getInstrumentationPackage(implementationTitle);
        if (instrumentationPackage != null) {
            String internalClassName = className.replace('.', '/');
            byte[] bytes = instrumentationPackage.getClassBytes().get(internalClassName);
            if (bytes != null) {
                ClassAppender.getSystemClassAppender().appendClasses(classLoader, instrumentationPackage.newClasses,
                                                                            instrumentationPackage.newClassLoadOrder);
            } else {
                instrumentationPackage.getLogger()
                        .fine("Unable to find " + className + " in instrumentation package " + implementationTitle);
            }
        } else {
            Agent.LOG.log(Level.FINE, "Unable to find instrumentation package {0} for class {1}.", implementationTitle,
                                 className);
        }
    }

    public ClassReader getClassReader(Class<?> theClass) throws BenignClassReadException {
        WeaveInstrumentation weaveInstrumentation = theClass.getAnnotation(WeaveInstrumentation.class);
        if (weaveInstrumentation != null) {
            InstrumentationPackage instrumentationPackage =
                    instrumentationPackageNames.get(weaveInstrumentation.title());
            byte[] bytes = instrumentationPackage.getClassBytes().get(Type.getInternalName(theClass));
            if (bytes != null) {
                return new ClassReader(bytes);
            }
            throw new BenignClassReadException(theClass.getName()
                                                       + " is WeaveInstrumentation but could not be found in "
                                                       + weaveInstrumentation.title());
        }

        return null;
    }

    public void registerInstrumentationCloseable(String instrumentationName, Closeable closeable) {
        InstrumentationPackage instrumentationPackage = instrumentationPackageNames.get(instrumentationName);
        if (instrumentationPackage == null) {
            Agent.LOG.log(Level.INFO, "Unable to register closeable {1} for missing instrumentationPackage {0}",
                                 instrumentationName, closeable);

            return;
        }
        instrumentationPackage.addCloseable(closeable);
    }

    private static class BootstrapClassTransformer extends WeavingClassTransformer {
        protected BootstrapClassTransformer(InstrumentationPackage instrumentationPackage) {
            super(instrumentationPackage);
        }

        protected byte[] doTransform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                     ProtectionDomain protectionDomain, byte[] classfileBuffer,
                                     InstrumentationContext context, OptimizedClassMatcher.Match match)
                throws Exception {
            if (loader == null) {
                loader = ClassLoader.getSystemClassLoader();
            }
            return super.doTransform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer, context,
                                            match);
        }
    }
}