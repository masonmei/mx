package com.newrelic.agent.commands;

import java.util.Map;

import com.newrelic.agent.IRPMService;

public abstract interface Command {
    public abstract String getName();

    public abstract Map process(IRPMService paramIRPMService, Map paramMap) throws CommandException;
}