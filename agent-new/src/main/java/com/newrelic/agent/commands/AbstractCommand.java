package com.newrelic.agent.commands;

public abstract class AbstractCommand implements Command {
    private final String commandName;

    public AbstractCommand(String commandName) {
        this.commandName = commandName;
    }

    public final String getName() {
        return this.commandName;
    }
}