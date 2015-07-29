package com.newrelic.agent;

public class TracerFactoryException extends Exception
{
  private static final long serialVersionUID = -6103280171903439862L;

  public TracerFactoryException(String message)
  {
    super(message);
  }

  public TracerFactoryException(String message, Exception e) {
    super(message, e);
  }
}