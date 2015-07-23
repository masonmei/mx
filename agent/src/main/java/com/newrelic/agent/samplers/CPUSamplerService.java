package com.newrelic.agent.samplers;

import java.io.File;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;

public class CPUSamplerService extends AbstractService implements HarvestListener {
    private final boolean enabled;
    private final IAgentLogger logger;
    private volatile AbstractCPUSampler cpuSampler;

    public CPUSamplerService() {
        super(CPUSamplerService.class.getSimpleName());
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        enabled = config.isCpuSamplingEnabled();
        logger = Agent.LOG.getChildLogger(getClass());
        if (!enabled) {
            logger.info("CPU Sampling is disabled");
        }
    }

    protected void doStart() {
        if (enabled) {
            cpuSampler = createCPUSampler();
            if (cpuSampler != null) {
                logger.fine("Started CPU Sampler");
                ServiceFactory.getHarvestService().addHarvestListener(this);
            }
        }
    }

    protected void doStop() {
        if (cpuSampler != null) {
            ServiceFactory.getHarvestService().removeHarvestListener(this);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        if (cpuSampler != null) {
            cpuSampler.recordCPU(statsEngine);
        }
    }

    public void afterHarvest(String appName) {
    }

    private AbstractCPUSampler createCPUSampler() {
        try {
            ClassLoader.getSystemClassLoader().loadClass("com.sun.management.OperatingSystemMXBean");
            return new CPUHarvester();
        } catch (Exception e) {
            try {
                int pid = ServiceFactory.getEnvironmentService().getProcessPID();
                File procStatFile = new File("/proc/" + pid + "/stat");
                if (procStatFile.exists()) {
                    return new ProcStatCPUSampler(procStatFile);
                }

                String osName = System.getProperty("os.name");
                if ("windows".equals(osName.toLowerCase())) {
                    logger.warning("CPU sampling is currently unsupported on Windows platforms for non-Sun JVMs");
                    return null;
                }
            } catch (Exception ex) {
                logger.warning("An error occurred starting the CPU sampler");
                logger.log(Level.FINER, "CPU sampler error", ex);
                return null;
            }

            logger.warning("CPU sampling is currently only supported in Sun JVMs");
        }
        return null;
    }
}