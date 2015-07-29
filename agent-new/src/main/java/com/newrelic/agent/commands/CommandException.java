package com.newrelic.agent.commands;

public class CommandException extends Exception
{
  private static final long serialVersionUID = 2152047474983639450L;

  public CommandException(String message)
  {
    super(message);
  }

  public CommandException(String message, Throwable t) {
    super(message, t);
  }
}