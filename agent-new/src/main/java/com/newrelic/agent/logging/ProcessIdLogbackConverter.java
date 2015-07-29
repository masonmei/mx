package com.newrelic.agent.logging;

import com.newrelic.deps.ch.qos.logback.classic.pattern.ClassicConverter;
import com.newrelic.deps.ch.qos.logback.classic.spi.ILoggingEvent;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

public class ProcessIdLogbackConverter extends ClassicConverter
{
  private final String pid;

  public ProcessIdLogbackConverter()
  {
    this.pid = Integer.toString(getProcessId());
  }

  private static int getProcessId() {
    String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
    String[] split = runtimeName.split("@");
    if (split.length > 1) {
      return Integer.parseInt(split[0]);
    }
    return 0;
  }

  public String convert(ILoggingEvent event)
  {
    try
    {
      return this.pid; } catch (Exception e) {
    }
    return null;
  }
}