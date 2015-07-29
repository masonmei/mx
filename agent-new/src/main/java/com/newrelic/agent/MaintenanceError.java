package com.newrelic.agent;

public class MaintenanceError extends Exception
{
  private static final long serialVersionUID = 8391541783636377551L;

  public MaintenanceError(String message)
  {
    super(message);
  }
}