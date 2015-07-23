package com.newrelic.agent.commands;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IRPMService;

public final class DisabledCommand extends AbstractCommand {
    private final String errorMessage;

    public DisabledCommand(String name) {
        this(name, MessageFormat.format("Command \"{0}\" is disabled", new Object[] {name}));
    }

    public DisabledCommand(String name, String errorMessage) {
        super(name);
        this.errorMessage = errorMessage;
    }

    public Map process(IRPMService rpmService, Map arguments) throws CommandException {
        Agent.LOG.log(Level.INFO, errorMessage);
        Map map = new HashMap();
        map.put("error", errorMessage);

        return map;
    }
}