package com.newrelic.agent.commands;

import java.util.Collections;
import java.util.Map;

import com.newrelic.agent.IRPMService;

public class PingCommand implements Command {
    public static final String COMMAND_NAME = "ping";

    public Map process(IRPMService rpmService, Map arguments) throws CommandException {
        return Collections.EMPTY_MAP;
    }

    public String getName() {
        return "ping";
    }
}