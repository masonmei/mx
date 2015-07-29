package com.newrelic.agent;

public class InternalLimitExceeded extends ServerCommandException
{
  private static final long serialVersionUID = -6876385842601935066L;

  public InternalLimitExceeded(String message)
  {
    super(message);
  }
}