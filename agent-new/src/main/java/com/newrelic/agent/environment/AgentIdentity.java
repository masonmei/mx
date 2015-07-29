package com.newrelic.agent.environment;

import com.newrelic.agent.Agent;
import com.newrelic.agent.logging.IAgentLogger;
import java.util.logging.Level;

public final class AgentIdentity
{
  private static final String UNKNOWN_DISPATCHER = "Unknown";
  private final String dispatcher;
  private final String dispatcherVersion;
  private final Integer serverPort;
  private final String instanceName;

  public AgentIdentity(String dispatcher, String dispatcherVersion, Integer serverPort, String instanceName)
  {
    this.dispatcher = (dispatcher == null ? "Unknown" : dispatcher);
    this.dispatcherVersion = dispatcherVersion;
    this.serverPort = serverPort;
    this.instanceName = instanceName;
  }

  public String getDispatcher() {
    return this.dispatcher;
  }

  public String getDispatcherVersion() {
    return this.dispatcherVersion;
  }

  public Integer getServerPort()
  {
    return this.serverPort;
  }

  public String getInstanceName()
  {
    return this.instanceName;
  }

  public boolean isServerInfoSet() {
    return (this.dispatcher != null) && (!"Unknown".equals(this.dispatcher)) && (this.dispatcherVersion != null);
  }

  private boolean isDispatcherNameNotSet() {
    return (this.dispatcher == null) || ("Unknown".equals(this.dispatcher));
  }

  private boolean isDispatcherVersionNotSet() {
    return this.dispatcherVersion == null;
  }

  public AgentIdentity createWithNewServerPort(Integer port)
  {
    if (this.serverPort == null) {
      return new AgentIdentity(this.dispatcher, this.dispatcherVersion, port, this.instanceName);
    }
    if (!this.serverPort.equals(port)) {
      Agent.LOG.log(Level.FINER, "Port is already {0}.  Ignore call to set it to {1}.", new Object[] { this.serverPort, port });
    }
    return null;
  }

  public AgentIdentity createWithNewInstanceName(String name)
  {
    if (this.instanceName == null) {
      return new AgentIdentity(this.dispatcher, this.dispatcherVersion, this.serverPort, name);
    }
    if (!this.instanceName.equals(name)) {
      Agent.LOG.log(Level.FINER, "Instance Name is already {0}.  Ignore call to set it to {1}.", new Object[] { this.instanceName, name });
    }

    return null;
  }

  public AgentIdentity createWithNewDispatcher(String dispatcherName, String version)
  {
    if (isServerInfoSet()) {
      Agent.LOG.log(Level.FINER, "Dispatcher is already {0}:{1}.  Ignore call to set it to {2}:{3}.", new Object[] { getDispatcher(), getDispatcherVersion(), dispatcherName, version });

      return null;
    }

    if ((isDispatcherNameNotSet()) && (isDispatcherVersionNotSet()))
    {
      if (dispatcherName == null) {
        dispatcherName = this.dispatcher;
      }
      if (version == null) {
        version = this.dispatcherVersion;
      }

      return new AgentIdentity(dispatcherName, version, this.serverPort, this.instanceName);
    }if (isDispatcherNameNotSet()) {
      Agent.LOG.log(Level.FINER, "Dispatcher previously set to {0}:{1}. Ignoring new version {3} but setting name to {2}.", new Object[] { getDispatcher(), getDispatcherVersion(), dispatcherName, version });

      return createWithNewDispatcherName(dispatcherName);
    }

    Agent.LOG.log(Level.FINER, "Dispatcher previously set to {0}:{1}. Ignoring new name {2} but setting version to {3}.", new Object[] { getDispatcher(), getDispatcherVersion(), dispatcherName, version });

    return createWithNewDispatcherVersion(version);
  }

  private AgentIdentity createWithNewDispatcherVersion(String version)
  {
    return new AgentIdentity(this.dispatcher, version, this.serverPort, this.instanceName);
  }

  private AgentIdentity createWithNewDispatcherName(String name)
  {
    if (name == null) {
      name = this.dispatcher;
    }

    return new AgentIdentity(name, this.dispatcherVersion, this.serverPort, this.instanceName);
  }
}