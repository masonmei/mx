package com.newrelic.agent.commands;

import com.newrelic.agent.IRPMService;
import java.util.Map;

public abstract interface Command
{
  public abstract String getName();

  public abstract Map process(IRPMService paramIRPMService, Map paramMap)
    throws CommandException;
}