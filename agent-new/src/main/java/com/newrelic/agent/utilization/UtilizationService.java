package com.newrelic.agent.utilization;

import java.lang.management.ManagementFactory;
import java.text.MessageFormat;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.Hostname;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.DefaultThreadFactory;

public class UtilizationService extends AbstractService {
    public static final String DETECT_AWS_KEY = "utilization.detect_aws";
    public static final String DETECT_DOCKER_KEY = "utilization.detect_docker";
    private static final String THREAD_NAME = "New Relic Utilization Service";
    private static final ExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory("New Relic Utilization Service", true));
    private static final AWS aws = new AWS();
    private final String hostName;
    private final boolean isLinux;
    private final boolean detectAws;
    private final boolean detectDocker;
    private volatile UtilizationData utilizationData = UtilizationData.EMPTY;
    private Future<UtilizationData> future = null;

    public UtilizationService() {
        super(UtilizationService.class.getSimpleName());
        AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();

        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        this.detectAws = ((Boolean) config.getValue("utilization.detect_aws", Boolean.TRUE)).booleanValue();
        this.detectDocker = ((Boolean) config.getValue("utilization.detect_docker", Boolean.TRUE)).booleanValue();

        this.hostName = Hostname.getHostname(Agent.LOG, agentConfig);
        this.isLinux = isLinuxOs();
    }

    private static boolean isLinuxOs() {
        String os = ManagementFactory.getOperatingSystemMXBean().getName();
        boolean outcome = (os != null) && (!os.startsWith("Windows")) && (!os.startsWith("Mac"));
        Agent.LOG.log(Level.FINEST, "Docker info is {1} gathered because OS is {0}.",
                             new Object[] {os, outcome ? "" : "not"});
        return outcome;
    }

    public boolean isEnabled() {
        return true;
    }

    protected void doStart() throws Exception {
        scheduleUtilizationTask();
    }

    protected void doStop() throws Exception {
        executor.shutdownNow();
    }

    private void scheduleUtilizationTask() {
        this.future = executor.submit(new UtilizationTask());
    }

    public UtilizationData updateUtilizationData() {
        if (this.future == null) {
            this.future = executor.submit(new UtilizationTask());
        }

        try {
            this.utilizationData = ((UtilizationData) this.future.get(1000L, TimeUnit.MILLISECONDS));
            this.future = null;
        } catch (InterruptedException e) {
            cleanupAndLogThrowable(e);
        } catch (ExecutionException e) {
            cleanupAndLogThrowable(e);
        } catch (TimeoutException e) {
            cleanupAndlogTimeout();
        } catch (Throwable t) {
            cleanupAndLogThrowable(t);
        }

        return this.utilizationData;
    }

    private void cleanupAndlogTimeout() {
        MemoryData.cleanup();
        Agent.LOG.log(Level.FINER, "Utilization task timed out. Returning cached utilization data.");
    }

    private void cleanupAndLogThrowable(Throwable t) {
        MemoryData.cleanup();
        Agent.LOG.log(Level.FINEST, MessageFormat
                                            .format("Utilization task exception. Returning cached utilization data. "
                                                            + "{0}",
                                                           new Object[] {t}));
    }

    protected AWS.AwsData getAwsData() {
        return aws.getAwsData();
    }

    protected String getDockerContainerId() {
        return DockerData.getDockerContainerId(this.isLinux);
    }

    class UtilizationTask implements Callable<UtilizationData> {
        UtilizationTask() {
        }

        public UtilizationData call() throws Exception {
            return doUpdateUtilizationData();
        }

        private UtilizationData doUpdateUtilizationData() {
            int processorCount = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
            String containerId =
                    UtilizationService.this.detectDocker ? UtilizationService.this.getDockerContainerId() : null;

            AWS.AwsData awsData =
                    UtilizationService.this.detectAws ? UtilizationService.this.getAwsData() : AWS.AwsData.EMPTY_DATA;

            long total_ram_mib = MemoryData.getTotalRamInMib();
            return new UtilizationData(UtilizationService.this.hostName, processorCount, containerId, awsData,
                                              total_ram_mib);
        }
    }
}