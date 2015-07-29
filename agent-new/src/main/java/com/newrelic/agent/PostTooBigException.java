package com.newrelic.agent;

public class PostTooBigException extends IgnoreSilentlyException
{
  private static final long serialVersionUID = 7001395828662633469L;

  public PostTooBigException(String message)
  {
    super(message);
  }
}