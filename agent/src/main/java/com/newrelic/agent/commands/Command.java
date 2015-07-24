package com.newrelic.agent.commands;

import java.util.Map;

import com.newrelic.agent.IRPMService;

public interface Command {
    String getName();

    Map process(IRPMService paramIRPMService, Map paramMap) throws CommandException;
}