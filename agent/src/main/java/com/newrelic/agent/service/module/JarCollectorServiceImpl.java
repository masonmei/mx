package com.newrelic.agent.service.module;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import com.google.common.collect.Maps;
import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.JarCollectorConfig;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;

public class JarCollectorServiceImpl extends AbstractService implements JarCollectorService, HarvestListener {
    private final JarCollectorServiceProcessor processor = new JarCollectorServiceProcessor();
    private final String defaultApp;
    private final boolean enabled;
    private final AtomicReference<Map<String, URL>> queuedJars = new AtomicReference(newUrlMap());
    private long lastAllJarFlush = 0L;

    public JarCollectorServiceImpl() {
        super(HarvestListener.class.getSimpleName());

        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        defaultApp = config.getApplicationName();

        JarCollectorConfig jarCollectorConfig = config.getJarCollectorConfig();
        enabled = jarCollectorConfig.isEnabled();
    }

    private Map<String, URL> newUrlMap() {
        return Maps.newConcurrentMap();
    }

    public final boolean isEnabled() {
        return enabled;
    }

    protected void doStart() throws Exception {
        if (enabled) {
            ServiceFactory.getHarvestService().addHarvestListener(this);
        }
    }

    protected void doStop() throws Exception {
        ServiceFactory.getHarvestService().removeHarvestListener(this);
    }

    private boolean needToSendAllJars() {
        return ServiceFactory.getRPMService().getConnectionTimestamp() > lastAllJarFlush;
    }

    public synchronized void beforeHarvest(String pAppName, StatsEngine pStatsEngine) {
        if (!defaultApp.equals(pAppName)) {
            return;
        }

        Agent.LOG.log(Level.FINER, "Harvesting Modules");

        boolean sendAll = needToSendAllJars();

        Map urls = (Map) queuedJars.getAndSet(newUrlMap());

        List<Jar> jars = processor.processModuleData(urls.values(), sendAll);

        if (sendAll) {
            lastAllJarFlush = System.nanoTime();
        }

        if (Agent.LOG.isLoggable(Level.FINEST)) {
            StringBuilder sb = new StringBuilder();
            for (Jar jar : jars) {
                sb.append("   ");
                sb.append(jar.getName());
                sb.append(":");
                sb.append(jar.getVersion());
            }
            Agent.LOG.log(Level.FINEST, "Sending jars: " + sb.toString());
        }

        if (!jars.isEmpty()) {
            try {
                ServiceFactory.getRPMService(pAppName).sendModules(jars);
            } catch (Exception e) {
                Agent.LOG.log(Level.FINE, MessageFormat.format("Unable to send {0} jar(s). Will attempt next harvest.",
                                                                      new Object[] {Integer.valueOf(jars.size())}));

                ((Map) queuedJars.get()).putAll(urls);
            }
        }
    }

    public void afterHarvest(String pAppName) {
    }

    Map<String, URL> getQueuedJars() {
        return (Map) queuedJars.get();
    }

    void addUrls(URL[] urls) {
        if (enabled) {
            for (URL url : urls) {
                if ("jar".equals(url.getProtocol())) {
                    String path = url.getFile();
                    if (!((Map) queuedJars.get()).containsKey(path)) {
                        int index = path.lastIndexOf(".jar");
                        if (index > 0) {
                            path = path.substring(0, index + ".jar".length());
                        }
                        try {
                            URL newUrl = new URL(path);
                            ((Map) queuedJars.get()).put(url.getPath(), newUrl);
                        } catch (MalformedURLException e) {
                            Agent.LOG.log(Level.FINEST, e, "Error parsing jar: {0}", new Object[] {e.getMessage()});
                        }
                    }
                } else if (url.getFile().endsWith(".jar")) {
                    ((Map) queuedJars.get()).put(url.getFile(), url);
                } else {
                    int jarIndex = url.getFile().lastIndexOf(".jar");
                    if (jarIndex > 0) {
                        String path = url.getFile().substring(0, jarIndex + ".jar".length());

                        if (!((Map) queuedJars.get()).containsKey(path)) {
                            try {
                                URL newUrl = new URL(url.getProtocol(), url.getHost(), path);
                                ((Map) queuedJars.get()).put(path, newUrl);
                            } catch (MalformedURLException e) {
                                Agent.LOG.log(Level.FINEST, e, "Error parsing jar: {0}", new Object[] {e.getMessage()});
                            }
                        }
                    }
                }
            }
        }
    }

    public ClassMatchVisitorFactory getSourceVisitor() {
        return new ClassMatchVisitorFactory() {
            public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined,
                                                     ClassReader reader, ClassVisitor cv,
                                                     InstrumentationContext context) {
                if ((enabled) && (null != context.getProtectionDomain()) && (null != context.getProtectionDomain()
                                                                                             .getCodeSource()) && (null
                                                                                                                           != context.getProtectionDomain()
                                                                                                                                      .getCodeSource()
                                                                                                                                      .getLocation())) {
                    addUrls(new URL[] {context.getProtectionDomain().getCodeSource().getLocation()});
                }
                return null;
            }
        };
    }
}