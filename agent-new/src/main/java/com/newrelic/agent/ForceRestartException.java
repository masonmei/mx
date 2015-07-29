package com.newrelic.agent;

public class ForceRestartException extends ServerCommandException
{
  private static final long serialVersionUID = 7001395828662633469L;

  public ForceRestartException(String message)
  {
    super(message);
  }
}