package com.newrelic.agent;

public class IgnoreSilentlyException extends Exception
{
  private static final long serialVersionUID = 7001395828662633469L;

  public IgnoreSilentlyException(String message)
  {
    super(message);
  }
}