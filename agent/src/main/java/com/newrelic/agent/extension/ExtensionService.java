//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.extension;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentJarHelper;
import com.newrelic.agent.config.ConfigFileHelper;
import com.newrelic.agent.extension.ExtensionParsers.ExtensionParser;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.agent.instrumentation.custom.ClassRetransformer;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.jmx.JmxService;
import com.newrelic.agent.reinstrument.ReinstrumentResult;
import com.newrelic.agent.reinstrument.ReinstrumentUtils;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.Service;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.deps.com.google.common.base.Predicate;
import com.newrelic.deps.com.google.common.collect.Collections2;
import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.com.google.common.collect.Sets;

public class ExtensionService extends AbstractService implements HarvestListener {
    private final Map<String, Extension> internalExtensions = Maps.newHashMap();
    private final List<ExtensionClassAndMethodMatcher> pointCuts = Lists.newArrayList();
    private final Collection<File> weaveExtensions = Lists.newArrayList();
    private final List<Service> services = Lists.newArrayList();
    private final List<ConfigurationConstruct> constructs = Lists.newArrayList();
    private ExtensionParsers extensionParsers;
    private volatile Set<Extension> extensions = Collections.emptySet();
    private long lastReloaded = 0L;
    private long lastReloadedWeaveInstrumentation = 0L;
    private int elementCount = -1;
    private int weaveElementCount = 0;

    public ExtensionService() {
        super(ExtensionService.class.getSimpleName());
    }

    public boolean isEnabled() {
        return true;
    }

    protected void doStart() {
        if (this.isEnabled()) {
            this.extensionParsers = new ExtensionParsers(this.constructs);

            try {
                this.initializeBuiltInExtensions();
                this.loadExtensionJars();
                this.reloadCustomExtensionsIfModified();
                this.reloadWeaveInstrumentationIfModified();
            } catch (NoSuchMethodError var2) {
                Agent.LOG.error("Unable to initialize agent extensions.  The likely cause is duplicate copies of "
                                        + "javax.xml libraries.");
                Agent.LOG.log(Level.FINE, var2.toString(), var2);
            }
        }

    }

    protected void doStop() {
        this.internalExtensions.clear();
        this.pointCuts.clear();
        this.weaveExtensions.clear();
        Iterator i$ = this.services.iterator();

        while (i$.hasNext()) {
            Service service = (Service) i$.next();

            try {
                service.stop();
            } catch (Exception var5) {
                String msg = MessageFormat.format("Unable to stop extension service \"{0}\" - {1}",
                                                         new Object[] {service.getName(), var5.toString()});
                Agent.LOG.severe(msg);
                this.getLogger().log(Level.FINE, msg, var5);
            }
        }

        this.services.clear();
    }

    public void beforeHarvest(String pAppName, StatsEngine pStatsEngine) {
    }

    public void afterHarvest(String pAppName) {
        if (ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName().equals(pAppName)) {
            if (ServiceFactory.getAgent().getInstrumentation().isRetransformClassesSupported()) {
                this.reloadCustomExtensionsIfModified();
                this.reloadWeaveInstrumentationIfModified();
            } else {
                Agent.LOG.log(Level.FINEST, "Retransformation is not supported - not reloading extensions.");
            }

        }
    }

    protected void addInternalExtensionForTesting(Extension ext) {
        this.internalExtensions.put(ext.getName(), ext);
    }

    private void initializeBuiltInExtensions() {
        String jarFileName = AgentJarHelper.getAgentJarFileName();
        if (jarFileName == null) {
            this.getLogger().log(Level.SEVERE, "Unable to find the agent jar file");
        } else {
            try {
                JarExtension e = JarExtension.create(this.getLogger(), this.extensionParsers, jarFileName);
                this.addJarExtensions(e);
            } catch (IOException var3) {
                this.getLogger().severe(MessageFormat.format("Unable to read extensions from the agent jar : {0}",
                                                                    new Object[] {var3.toString()}));
                this.getLogger().log(Level.FINER, "Extensions error", var3);
            }

        }
    }

    private void loadExtensionJars() {
        Collection jarExtensions = this.loadJarExtensions(this.getExtensionDirectory());
        Iterator i$ = jarExtensions.iterator();

        while (true) {
            JarExtension extension;
            do {
                if (!i$.hasNext()) {
                    return;
                }

                extension = (JarExtension) i$.next();
            } while (extension.isWeaveInstrumentation());

            try {
                Iterator t = extension.getClasses().iterator();

                while (t.hasNext()) {
                    Class clazz = (Class) t.next();
                    this.noticeExtensionClass(clazz);
                }

                this.addJarExtensions(extension);
            } catch (Throwable var6) {
                Agent.LOG.log(Level.INFO, "An error occurred adding extension {0} : {1}",
                                     new Object[] {extension.getFile(), var6.getMessage()});
                Agent.LOG.log(Level.FINEST, var6, var6.getMessage(), new Object[0]);
            }
        }
    }

    private void addJarExtensions(JarExtension jarExtension) {
        Iterator i$ = jarExtension.getExtensions().values().iterator();

        while (i$.hasNext()) {
            Extension extension = (Extension) i$.next();
            Extension validateExtension = this.validateExtension(extension, this.internalExtensions);
            if (validateExtension != null) {
                this.internalExtensions.put(extension.getName(), extension);
            }
        }

    }

    private void reloadCustomExtensionsIfModified() {
        File[] xmlFiles = this.getExtensionFiles(ExtensionFileTypes.XML.getFilter());
        File[] ymlFiles = this.getExtensionFiles(ExtensionFileTypes.YML.getFilter());
        boolean fileModified = xmlFiles.length + ymlFiles.length != this.elementCount;
        if (!fileModified) {
            File[] allExtensions = xmlFiles;
            int externalExtensions = xmlFiles.length;

            int oldExtensions;
            File jmxService;
            for (oldExtensions = 0; oldExtensions < externalExtensions; ++oldExtensions) {
                jmxService = allExtensions[oldExtensions];
                fileModified |= this.lastReloaded < jmxService.lastModified();
            }

            allExtensions = ymlFiles;
            externalExtensions = ymlFiles.length;

            for (oldExtensions = 0; oldExtensions < externalExtensions; ++oldExtensions) {
                jmxService = allExtensions[oldExtensions];
                fileModified |= this.lastReloaded < jmxService.lastModified();
            }
        }

        if (fileModified) {
            this.lastReloaded = System.currentTimeMillis();
            this.elementCount = xmlFiles.length + ymlFiles.length;
            this.pointCuts.clear();
            HashMap var11 = Maps.newHashMap(this.internalExtensions);
            this.loadValidExtensions(xmlFiles, this.extensionParsers.getXmlParser(), var11);
            this.loadValidExtensions(ymlFiles, this.extensionParsers.getYamlParser(), var11);
            HashSet var12 = Sets.newHashSet(var11.values());
            var12.removeAll(this.internalExtensions.values());
            Set var13 = this.extensions;
            this.extensions = Collections.unmodifiableSet(var12);
            JmxService var14 = ServiceFactory.getJmxService();
            if (var14 != null) {
                var14.reloadExtensions(var13, this.extensions);
            }

            Iterator retransformer = var11.values().iterator();

            while (retransformer.hasNext()) {
                Extension allLoadedClasses = (Extension) retransformer.next();
                this.pointCuts.addAll(allLoadedClasses.getInstrumentationMatchers());
            }

            ClassRetransformer var15 = ServiceFactory.getClassTransformerService().getLocalRetransformer();
            if (var15 != null) {
                Class[] var16 = ServiceFactory.getAgent().getInstrumentation().getAllLoadedClasses();
                var15.setClassMethodMatchers(this.pointCuts);
                Set classesToRetransform = InstrumentationContext.getMatchingClasses(var15.getMatchers(), var16);
                ReinstrumentUtils
                        .checkClassExistsAndRetransformClasses(new ReinstrumentResult(), Collections.EMPTY_LIST, null,
                                                                      classesToRetransform);
            }
        }

    }

    private void reloadWeaveInstrumentationIfModified() {
        File[] jarFiles = this.getExtensionFiles(ExtensionFileTypes.JAR.getFilter());
        Collection weaveFiles = Collections2.filter(Arrays.asList(jarFiles), new Predicate<File>() {
            public boolean apply(File extension) {
                return JarExtension.isWeaveInstrumentation(extension);
            }
        });
        boolean fileModified = weaveFiles.size() != this.weaveElementCount;
        File file;
        if (!fileModified) {
            for (Iterator contextManager = weaveFiles.iterator(); contextManager.hasNext();
                 fileModified |= this.lastReloadedWeaveInstrumentation < file.lastModified()) {
                file = (File) contextManager.next();
            }
        }

        if (fileModified) {
            this.lastReloadedWeaveInstrumentation = System.currentTimeMillis();
            this.weaveElementCount = weaveFiles.size();
            this.weaveExtensions.clear();
            this.weaveExtensions.addAll(weaveFiles);
            InstrumentationContextManager contextManager1 =
                    ServiceFactory.getClassTransformerService().getContextManager();
            if (contextManager1 != null) {
                contextManager1.getClassWeaverService().reloadInstrumentationPackages(this.weaveExtensions).run();
            }

            Agent.LOG.finer("Weave extension jars: " + this.weaveExtensions);
        }

    }

    private File[] getExtensionFiles(FileFilter filter) {
        File directory = this.getExtensionDirectory();
        return directory == null ? new File[0] : directory.listFiles(filter);
    }

    private File getExtensionDirectory() {
        AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
        String configDirName = (String) agentConfig.getProperty("extensions.dir");
        if (configDirName == null) {
            configDirName = ConfigFileHelper.getNewRelicDirectory() + File.separator + "extensions";
        }

        File configDir = new File(configDirName);
        if (!configDir.exists()) {
            Agent.LOG.log(Level.FINE, "The extension directory " + configDir.getAbsolutePath() + " does not exist.");
            configDir = null;
        } else if (!configDir.isDirectory()) {
            Agent.LOG.log(Level.WARNING,
                                 "The extension directory " + configDir.getAbsolutePath() + " is not a directory.");
            configDir = null;
        } else if (!configDir.canRead()) {
            Agent.LOG
                    .log(Level.WARNING, "The extension directory " + configDir.getAbsolutePath() + " is not readable.");
            configDir = null;
        }

        return configDir;
    }

    private void loadValidExtensions(File[] files, ExtensionParser parser, HashMap<String, Extension> extensions) {
        if (files != null) {
            File[] arr$ = files;
            int len$ = files.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                File file = arr$[i$];
                this.getLogger().log(Level.FINER, MessageFormat.format("Reading custom extension file {0}",
                                                                              new Object[] {file.getAbsolutePath()}));

                try {
                    Extension ex = this.readExtension(parser, file);
                    ex = this.validateExtension(ex, extensions);
                    if (ex != null) {
                        extensions.put(ex.getName(), ex);
                    } else {
                        this.getLogger().log(Level.WARNING, "Extension in file " + file.getAbsolutePath()
                                                                    + " could not be read in.");
                    }
                } catch (Exception var9) {
                    this.getLogger()
                            .severe("Unable to parse extension " + file.getAbsolutePath() + ".  " + var9.toString());
                    this.getLogger().log(Level.FINE, var9.toString(), var9);
                }
            }
        }

    }

    private Extension readExtension(ExtensionParser parser, File file) throws Exception {
        FileInputStream iStream = new FileInputStream(file);

        Extension var4;
        try {
            var4 = parser.parse(ClassLoader.getSystemClassLoader(), iStream, true);
        } finally {
            iStream.close();
        }

        return var4;
    }

    protected Extension validateExtension(Extension extension, Map<String, Extension> existingExtensions) {
        String name = extension.getName();
        if (name != null && name.length() != 0) {
            double version = extension.getVersionNumber();
            Extension existing = (Extension) existingExtensions.get(name);
            if (existing == null) {
                this.getLogger().log(Level.FINER, MessageFormat.format("Adding extension with name {0} and version {1}",
                                                                              new Object[] {name,
                                                                                                   Double.valueOf(version)
                                                                                                           .toString()}));
                return extension;
            }

            if (version > existing.getVersionNumber()) {
                this.getLogger().log(Level.FINER, MessageFormat.format("Updating extension with name {0} to version "
                                                                               + "{1}", new Object[] {name,
                                                                                                             Double.valueOf(version)
                                                                                                                     .toString()}));
                return extension;
            }

            this.getLogger().log(Level.FINER, MessageFormat.format("Additional extension with name {0} and version {1} "
                                                                           + "being ignored. Another file with name "
                                                                           + "and " + "version already read in.",
                                                                          new Object[] {name, Double.valueOf(version)
                                                                                                      .toString()}));
        }

        return null;
    }

    private void noticeExtensionClass(Class<?> clazz) {
        this.getLogger().finest(MessageFormat.format("Noticed extension class {0}", new Object[] {clazz.getName()}));
        if (Service.class.isAssignableFrom(clazz)) {
            try {
                this.addService((Service) clazz.getConstructor(new Class[0]).newInstance(new Object[0]));
            } catch (Exception var3) {
                this.getLogger().severe(MessageFormat.format("Unable to instantiate extension service \"{0}\"",
                                                                    new Object[] {clazz.getName()}));
                this.getLogger().log(Level.FINE, "Unable to instantiate service", var3);
            }
        }

    }

    private void addService(Service service) {
        String msg = MessageFormat.format("Noticed extension service \"{0}\"", new Object[] {service.getName()});
        this.getLogger().finest(msg);
        if (service.isEnabled()) {
            this.services.add(service);
            msg = MessageFormat.format("Starting extension service \"{0}\"", new Object[] {service.getName()});
            this.getLogger().finest(msg);

            try {
                service.start();
            } catch (Exception var4) {
                msg = MessageFormat.format("Unable to start extension service \"{0}\" - {1}",
                                                  new Object[] {service.getName(), var4.toString()});
                this.getLogger().severe(msg);
                this.getLogger().log(Level.FINE, msg, var4);
            }

        }
    }

    private Collection<JarExtension> loadJarExtensions(File jarDirectory) {
        return (Collection) (jarDirectory != null && jarDirectory.exists() ? (jarDirectory.isDirectory()
                                                                                      ? this.loadJars(jarDirectory
                                                                                                              .listFiles(ExtensionFileTypes.JAR
                                                                                                                                 .getFilter()))
                                                                                      : (jarDirectory.exists()
                                                                                                 ? this.loadJars(new File[] {jarDirectory})
                                                                                                 : Collections
                                                                                                           .emptyList
                                                                                                                    ()))
                                     : Collections.emptyList());
    }

    private Collection<JarExtension> loadJars(File[] jarFiles) {
        ArrayList extensions = new ArrayList();
        File[] arr$ = jarFiles;
        int len$ = jarFiles.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            File file = arr$[i$];

            try {
                JarExtension ex = JarExtension.create(this.getLogger(), this.extensionParsers, file);
                extensions.add(ex);
            } catch (IOException var8) {
                Agent.LOG.severe("Unable to load extension " + file.getName());
                Agent.LOG.log(Level.FINER, var8.toString(), var8);
            }
        }

        return Collections.unmodifiableCollection(extensions);
    }

    public final List<ExtensionClassAndMethodMatcher> getEnabledPointCuts() {
        return this.pointCuts;
    }

    public void addConstruct(ConfigurationConstruct construct) {
        this.constructs.add(construct);
    }

    public final Map<String, Extension> getInternalExtensions() {
        return Collections.unmodifiableMap(this.internalExtensions);
    }

    public final Set<Extension> getExtensions() {
        return this.extensions;
    }

    public Collection<File> getWeaveExtensions() {
        return this.weaveExtensions;
    }
}
