package com.newrelic.agent.environment;

import java.lang.management.ManagementFactory;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.logging.AgentLogManager;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;

public class EnvironmentServiceImpl extends AbstractService implements EnvironmentService {
    private final int processPID;
    private final Environment environment;

    public EnvironmentServiceImpl() {
        super(EnvironmentService.class.getSimpleName());

        processPID = initProcessPID();
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        environment = initEnvironment(config);
    }

    protected void doStart() {
    }

    protected void doStop() {
    }

    public boolean isEnabled() {
        return true;
    }

    public int getProcessPID() {
        return processPID;
    }

    private int initProcessPID() {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        String[] split = runtimeName.split("@");
        if (split.length > 1) {
            return Integer.parseInt(split[0]);
        }
        return 0;
    }

    public Environment getEnvironment() {
        return environment;
    }

    private Environment initEnvironment(AgentConfig config) {
        String logFilePath = AgentLogManager.getLogFilePath();
        if (logFilePath == null) {
            logFilePath = "";
        }
        return new Environment(config, logFilePath);
    }
}