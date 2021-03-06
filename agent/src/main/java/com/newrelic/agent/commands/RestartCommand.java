package com.newrelic.agent.commands;

import java.util.Collections;
import java.util.Map;

import com.newrelic.agent.IRPMService;

public class RestartCommand extends AbstractCommand {
    public static final String COMMAND_NAME = "restart";

    public RestartCommand() {
        super(COMMAND_NAME);
    }

    public Map process(IRPMService rpmService, Map arguments) throws CommandException {
        rpmService.reconnect();
        return Collections.EMPTY_MAP;
    }
}