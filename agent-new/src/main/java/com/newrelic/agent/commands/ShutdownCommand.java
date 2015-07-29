package com.newrelic.agent.commands;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IAgent;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.logging.IAgentLogger;
import java.util.Collections;
import java.util.Map;

public class ShutdownCommand extends AbstractCommand
{
  public static final String COMMAND_NAME = "shutdown";
  private final IAgent agent;

  public ShutdownCommand(IAgent agent)
  {
    super("shutdown");
    this.agent = agent;
  }

  public Map process(IRPMService rpmService, Map arguments) throws CommandException
  {
    Agent.LOG.info("ShutdownCommand is shutting down the Agent");
    this.agent.shutdownAsync();
    return Collections.EMPTY_MAP;
  }
}