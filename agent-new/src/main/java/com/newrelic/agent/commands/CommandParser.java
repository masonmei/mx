package com.newrelic.agent.commands;

import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.IAgent;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.util.JSONException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class CommandParser extends AbstractService
  implements HarvestListener
{
  private final Map<String, Command> commands = new HashMap();
  private boolean enabled = true;

  public CommandParser()
  {
    super(CommandParser.class.getSimpleName());
  }

  public void addCommands(Command[] commands)
  {
    for (Command command : commands)
      this.commands.put(command.getName(), command);
  }

  public void beforeHarvest(String appName, StatsEngine statsEngine)
  {
    IRPMService rpmService = ServiceFactory.getRPMService(appName);
    List commands;
    try
    {
      commands = rpmService.getAgentCommands();
    } catch (Exception e) {
      getLogger().log(Level.FINE, "Unable to get agent commands - {0}", new Object[] { e.toString() });
      getLogger().log(Level.FINEST, e, e.toString(), new Object[0]);
      return;
    }

    Map commandResults = processCommands(rpmService, commands);
    try {
      rpmService.sendCommandResults(commandResults);
    } catch (Exception e) {
      String msg = MessageFormat.format("Unable to send agent command feedback.  Command results: {0}", new Object[] { commandResults.toString() });

      getLogger().fine(msg);
    }
  }

  public void afterHarvest(String appName)
  {
  }

  Command getCommand(String name) throws UnknownCommand
  {
    Agent.LOG.finer(MessageFormat.format("Process command \"{0}\"", new Object[] { name }));
    Command c = (Command)this.commands.get(name);
    if (c == null) {
      throw new UnknownCommand("Unknown command " + name);
    }
    return c;
  }

  Map<Long, Object> processCommands(IRPMService rpmService, List<List<?>> commands)
  {
    Map results = new HashMap();
    int count = 0;
    for (List agentCommand : commands) {
      if (agentCommand.size() == 2) {
        Object id = agentCommand.get(0);
        if ((id instanceof Number)) {
          try {
            Map commandMap = (Map)agentCommand.get(1);
            String name = (String)commandMap.get("name");
            Map args = (Map)commandMap.get("arguments");
            if (args == null) {
              args = Collections.EMPTY_MAP;
            }
            Command command = getCommand(name);
            Object returnValue = command.process(rpmService, args);
            results.put(Long.valueOf(((Number)id).longValue()), returnValue);
            getLogger().finer(MessageFormat.format("Agent command \"{0}\" return value: {1}", new Object[] { name, returnValue }));
          }
          catch (Exception e) {
            getLogger().severe(MessageFormat.format("Unable to parse command : {0}", new Object[] { e.toString() }));
            getLogger().fine(MessageFormat.format("Unable to parse command", new Object[] { e }));
            results.put(Long.valueOf(((Number)id).longValue()), new JSONException(e));
          }
        }
        else {
          invalidCommand(rpmService, count, "Invalid command id " + id, agentCommand);
        }
      }
      else
      {
        invalidCommand(rpmService, count, "Unable to parse command", agentCommand);
      }

      count++;
    }
    return results;
  }

  private void invalidCommand(IRPMService rpmService, int index, String message, List<?> agentCommand)
  {
    getLogger().severe(MessageFormat.format("Unable to parse command : {0} ({1})", new Object[] { message, agentCommand.toString() }));
  }

  public boolean isEnabled()
  {
    return this.enabled;
  }

  protected void doStart()
  {
    AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
    IAgent agent = ServiceFactory.getAgent();
    addCommands(new Command[] { new ShutdownCommand(agent), new RestartCommand() });
    setEnabled(config);
    if (isEnabled())
      ServiceFactory.getHarvestService().addHarvestListener(this);
    else
      getLogger().log(Level.CONFIG, "The command parser is disabled");
  }

  private void setEnabled(AgentConfig agentConfig)
  {
    try {
      Map props = (Map)agentConfig.getProperty("command_parser");
      if (props != null) {
        Boolean enabled = (Boolean)props.get("enabled");
        this.enabled = ((enabled != null) && (enabled.booleanValue()));
      }
    } catch (Throwable t) {
      getLogger().log(Level.SEVERE, "Unable to parse the command_parser section in newrelic.yml");
    }
  }

  protected void doStop()
  {
  }
}