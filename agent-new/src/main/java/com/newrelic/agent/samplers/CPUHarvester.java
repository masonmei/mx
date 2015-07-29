package com.newrelic.agent.samplers;

import com.newrelic.agent.util.TimeConversion;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;

public class CPUHarvester extends AbstractCPUSampler
{
  private final OperatingSystemMXBean osMBean;

  public CPUHarvester()
  {
    this.osMBean = ((OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean());
  }

  protected double getProcessCpuTime()
  {
    return TimeConversion.convertNanosToSeconds(this.osMBean.getProcessCpuTime());
  }
}