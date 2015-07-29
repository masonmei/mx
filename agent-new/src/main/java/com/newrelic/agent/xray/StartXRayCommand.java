package com.newrelic.agent.xray;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.commands.AbstractCommand;
import com.newrelic.agent.commands.CommandException;

public class StartXRayCommand extends AbstractCommand {
  public static final String COMMAND_NAME = "active_xray_sessions";
  private static final String DISABLED_MESSAGE = "The X-Ray service is disabled";
  private IXRaySessionService xRaySessionService;

  public StartXRayCommand(XRaySessionService xRaySessionService) {
    super("active_xray_sessions");
    this.xRaySessionService = xRaySessionService;
  }

  public Map<?, ?> process(IRPMService rpmService, Map arguments) throws CommandException {
    if (xRaySessionService.isEnabled()) {
      return processEnabled(rpmService, arguments);
    }
    return processDisabled(rpmService, arguments);
  }

  private Map processDisabled(IRPMService rpmService, Map arguments) {
    Agent.LOG.debug("The X-Ray service is disabled");
    try {
      xRaySessionService.stop();
    } catch (Exception e) {
      Agent.LOG.warning("Error disabling X-Ray Session service: " + e.getMessage());
    }
    return Collections.EMPTY_MAP;
  }

  private Map processEnabled(IRPMService rpmService, Map arguments) {
    Object xray_ids = arguments.remove("xray_ids");

    List xrayIds;
    if ((xray_ids instanceof List)) {
      xrayIds = (List) xray_ids;
    } else {
      xrayIds = Collections.emptyList();
    }
    return xRaySessionService.processSessionsList(xrayIds, rpmService);
  }
}