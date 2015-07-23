package com.newrelic.agent.environment;

import java.util.logging.Level;

import com.newrelic.agent.Agent;

public final class AgentIdentity {
    private static final String UNKNOWN_DISPATCHER = "Unknown";
    private final String dispatcher;
    private final String dispatcherVersion;
    private final Integer serverPort;
    private final String instanceName;

    public AgentIdentity(String dispatcher, String dispatcherVersion, Integer serverPort, String instanceName) {
        this.dispatcher = (dispatcher == null ? "Unknown" : dispatcher);
        this.dispatcherVersion = dispatcherVersion;
        this.serverPort = serverPort;
        this.instanceName = instanceName;
    }

    public String getDispatcher() {
        return dispatcher;
    }

    public String getDispatcherVersion() {
        return dispatcherVersion;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public boolean isServerInfoSet() {
        return (dispatcher != null) && (!"Unknown".equals(dispatcher)) && (dispatcherVersion != null);
    }

    private boolean isDispatcherNameNotSet() {
        return (dispatcher == null) || ("Unknown".equals(dispatcher));
    }

    private boolean isDispatcherVersionNotSet() {
        return dispatcherVersion == null;
    }

    public AgentIdentity createWithNewServerPort(Integer port) {
        if (serverPort == null) {
            return new AgentIdentity(dispatcher, dispatcherVersion, port, instanceName);
        }
        if (!serverPort.equals(port)) {
            Agent.LOG.log(Level.FINER, "Port is already {0}.  Ignore call to set it to {1}.",
                                 new Object[] {serverPort, port});
        }
        return null;
    }

    public AgentIdentity createWithNewInstanceName(String name) {
        if (instanceName == null) {
            return new AgentIdentity(dispatcher, dispatcherVersion, serverPort, name);
        }
        if (!instanceName.equals(name)) {
            Agent.LOG.log(Level.FINER, "Instance Name is already {0}.  Ignore call to set it to {1}.",
                                 new Object[] {instanceName, name});
        }

        return null;
    }

    public AgentIdentity createWithNewDispatcher(String dispatcherName, String version) {
        if (isServerInfoSet()) {
            Agent.LOG.log(Level.FINER, "Dispatcher is already {0}:{1}.  Ignore call to set it to {2}:{3}.",
                                 new Object[] {getDispatcher(), getDispatcherVersion(), dispatcherName, version});

            return null;
        }

        if ((isDispatcherNameNotSet()) && (isDispatcherVersionNotSet())) {
            if (dispatcherName == null) {
                dispatcherName = dispatcher;
            }
            if (version == null) {
                version = dispatcherVersion;
            }

            return new AgentIdentity(dispatcherName, version, serverPort, instanceName);
        }
        if (isDispatcherNameNotSet()) {
            Agent.LOG.log(Level.FINER,
                                 "Dispatcher previously set to {0}:{1}. Ignoring new version {3} but setting name to "
                                         + "{2}.",
                                 new Object[] {getDispatcher(), getDispatcherVersion(), dispatcherName, version});

            return createWithNewDispatcherName(dispatcherName);
        }

        Agent.LOG.log(Level.FINER,
                             "Dispatcher previously set to {0}:{1}. Ignoring new name {2} but setting version to {3}.",
                             new Object[] {getDispatcher(), getDispatcherVersion(), dispatcherName, version});

        return createWithNewDispatcherVersion(version);
    }

    private AgentIdentity createWithNewDispatcherVersion(String version) {
        return new AgentIdentity(dispatcher, version, serverPort, instanceName);
    }

    private AgentIdentity createWithNewDispatcherName(String name) {
        if (name == null) {
            name = dispatcher;
        }

        return new AgentIdentity(name, dispatcherVersion, serverPort, instanceName);
    }
}