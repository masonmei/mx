package com.newrelic.agent;

public class ForceDisconnectException extends ServerCommandException
{
  private static final long serialVersionUID = 7001395828662633469L;

  public ForceDisconnectException(String message)
  {
    super(message);
  }
}