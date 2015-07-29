package com.newrelic.agent.commands;

import com.newrelic.agent.IRPMService;
import java.util.Collections;
import java.util.Map;

public class RestartCommand extends AbstractCommand
{
  public static final String COMMAND_NAME = "restart";

  public RestartCommand()
  {
    super("restart");
  }

  public Map process(IRPMService rpmService, Map arguments) throws CommandException
  {
    rpmService.reconnect();
    return Collections.EMPTY_MAP;
  }
}