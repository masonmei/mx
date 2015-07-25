package com.newrelic.agent;

import java.lang.instrument.Instrumentation;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.install.ConfigInstaller;
import com.newrelic.agent.logging.AgentLogManager;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.service.ServiceManagerImpl;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.api.agent.NewRelicApiImplementation;
import com.newrelic.bootstrap.BootstrapAgent;

public final class Agent extends AbstractService implements IAgent {
    public static final IAgentLogger LOG = AgentLogManager.getLogger();
    public static final int ASM_LEVEL = 327680;
    private static final String AGENT_ENABLED_PROPERTY = "newrelic.config.agent_enabled";
    private static final boolean DEBUG = Boolean.getBoolean("newrelic.debug");
    private static final String VERSION = initVersion();

    private static final ClassLoader NR_CLASS_LOADER =
            BootstrapAgent.class.getClassLoader() == null ? ClassLoader.getSystemClassLoader()
                    : BootstrapAgent.class.getClassLoader();
    private static long agentPremainTime;
    private static volatile boolean canFastPath = true;
    private final Instrumentation instrumentation;
    private volatile boolean enabled = true;
    private volatile InstrumentationProxy instrumentationProxy;

    private Agent(Instrumentation instrumentation) {
        super(IAgent.class.getSimpleName());
        this.instrumentation = instrumentation;
    }

    public static String getVersion() {
        return VERSION;
    }

    public static ClassLoader getClassLoader() {
        return NR_CLASS_LOADER;
    }

    private static String initVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(Agent.class.getName());
            return bundle.getString("version");
        } catch (Throwable t) {
        }
        return "0.0";
    }

    public static boolean isDebugEnabled() {
        return DEBUG;
    }

    public static boolean canFastPath() {
        return canFastPath;
    }

    public static void disableFastPath() {
        if (canFastPath) {
            canFastPath = false;
        }
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        if (ServiceFactory.getServiceManager() != null) {
            LOG.warning("New Relic Agent is already running! Check if more than one -javaagent switch is used on the "
                                + "command line.");
            return;
        }
        String enabled = System.getProperty(AGENT_ENABLED_PROPERTY);
        if ((enabled != null) && (!Boolean.parseBoolean(enabled.toString()))) {
            LOG.warning("New Relic agent is disabled by a system property.");
            return;
        }
        String jvmName = System.getProperty("java.vm.name");
        if (jvmName.contains("Oracle JRockit")) {
            String msg = MessageFormat
                                 .format("New Relic agent {0} does not support the Oracle JRockit JVM. Please use a 2"
                                                 + ".21.x or earlier version of the agent. JVM  is: {1}.", getVersion(),
                                                jvmName);

            LOG.error(msg);
        }
        try {
            IAgent agent = new Agent(inst);
            ServiceManager serviceManager = new ServiceManagerImpl(agent);
            ServiceFactory.setServiceManager(serviceManager);

            if (ConfigInstaller
                        .isLicenseKeyEmpty(serviceManager.getConfigService().getDefaultAgentConfig().getLicenseKey())) {
                LOG.error("license_key is empty in the config. Not starting New Relic Agent.");
                return;
            }

            if (!serviceManager.getConfigService().getDefaultAgentConfig().isAgentEnabled()) {
                LOG.warning("agent_enabled is false in the config. Not starting New Relic Agent.");
                return;
            }

            serviceManager.start();

            LOG.info(MessageFormat.format("New Relic Agent v{0} has started", getVersion()));

            if (BootstrapAgent.isBootstrapClasspathFlagSet()) {
                LOG.info("The newrelic.bootstrap_classpath system property is deprecated.");
            }
            if (Agent.class.getClassLoader() == null) {
                LOG.info("Agent class loader is null which typically means the agent is loaded by the bootstrap class"
                                 + " loader.");
            } else {
                LOG.info("Agent class loader: " + Agent.class.getClassLoader());
            }

            if (serviceManager.getConfigService().getDefaultAgentConfig().isStartupTimingEnabled()) {
                recordPremainTime(serviceManager.getStatsService());
            }
        } catch (Throwable t) {
            String msg = MessageFormat.format("Unable to start New Relic agent: {0}", t);
            try {
                LOG.log(Level.SEVERE, msg, t);
            } catch (Throwable t2) {
            }
            System.err.println(msg);
            t.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion.startsWith("1.5")) {
            String msg = MessageFormat
                                 .format("Java version is: {0}.  This version of the New Relic Agent does not support"
                                                 + " Java 1.5.  Please use a 2.21.x or earlier version.",
                                                new Object[] {javaVersion});

            System.err.println("----------");
            System.err.println(msg);
            System.err.println("----------");
            return;
        }
        new AgentCommandLineParser().parseCommand(args);
    }

    public static long getAgentPremainTimeInMillis() {
        return agentPremainTime;
    }

    private static void recordPremainTime(StatsService statsService) {
        agentPremainTime = System.currentTimeMillis() - BootstrapAgent.getAgentStartTime();
        LOG.log(Level.INFO, "Premain startup complete in {0}ms", new Object[] {Long.valueOf(agentPremainTime)});
        statsService
                .doStatsWork(StatsWorks.getRecordResponseTimeWork("Supportability/Timing/Premain", agentPremainTime));

        Map environmentInfo =
                ImmutableMap.builder().put("Duration", Long.valueOf(agentPremainTime)).put("Version", getVersion())
                        .put("JRE Vendor", System.getProperty("java.vendor"))
                        .put("JRE Version", System.getProperty("java.version"))
                        .put("JVM Vendor", System.getProperty("java.vm.vendor"))
                        .put("JVM Version", System.getProperty("java.vm.version"))
                        .put("JVM Runtime Version", System.getProperty("java.runtime.version"))
                        .put("OS Name", System.getProperty("os.name"))
                        .put("OS Version", System.getProperty("os.version"))
                        .put("OS Arch", System.getProperty("os.arch"))
                        .put("Processors", Integer.valueOf(Runtime.getRuntime().availableProcessors()))
                        .put("Free Memory", Long.valueOf(Runtime.getRuntime().freeMemory()))
                        .put("Total Memory", Long.valueOf(Runtime.getRuntime().totalMemory()))
                        .put("Max Memory", Long.valueOf(Runtime.getRuntime().maxMemory())).build();

        LOG.log(Level.FINE, "Premain environment info: {0}", environmentInfo);
    }

    protected void doStart() {
        ConfigService configService = ServiceFactory.getConfigService();
        AgentConfig config = configService.getDefaultAgentConfig();
        AgentLogManager.configureLogger(config);

        logHostIp();
        LOG.info(MessageFormat.format("New Relic Agent v{0} is initializing...", getVersion()));

        enabled = config.isAgentEnabled();
        if (!enabled) {
            LOG.info("New Relic agent is disabled.");
        }

        instrumentationProxy = InstrumentationProxy.getInstrumentationProxy(instrumentation);

        initializeBridgeApis();

        final long startTime = System.currentTimeMillis();
        Runnable runnable = new Runnable() {
            public void run() {
                Agent.this.jvmShutdown(startTime);
            }
        };
        Thread shutdownThread = new Thread(runnable, "New Relic JVM Shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }

    private void initializeBridgeApis() {
        NewRelicApiImplementation.initialize();
        PrivateApiImpl.initialize(LOG);
    }

    private void logHostIp() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            LOG.info("Agent Host: " + address.getHostName() + " IP: " + address.getHostAddress());
        } catch (UnknownHostException e) {
            LOG.info("New Relic could not identify host/ip.");
        }
    }

    protected void doStop() {
    }

    public void shutdownAsync() {
        Runnable runnable = new Runnable() {
            public void run() {
                shutdown();
            }
        };
        Thread shutdownThread = new Thread(runnable, "New Relic Shutdown");
        shutdownThread.start();
    }

    private void jvmShutdown(long startTime) {
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        if ((config.isSendDataOnExit()) && (System.currentTimeMillis() - startTime
                                                    >= config.getSendDataOnExitThresholdInMillis())) {
            ServiceFactory.getHarvestService().harvestNow();
        }
        getLogger().info("JVM is shutting down");
        shutdown();
    }

    public synchronized void shutdown() {
        try {
            ServiceFactory.getServiceManager().stop();
            getLogger().info("New Relic Agent has shutdown");
        } catch (Exception e) {
            LOG.severe(MessageFormat.format("Error shutting down New Relic Agent: {0}", new Object[] {e}));
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public InstrumentationProxy getInstrumentation() {
        return instrumentationProxy;
    }
}