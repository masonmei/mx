package com.newrelic.agent.samplers;

import java.lang.management.ManagementFactory;

import com.newrelic.agent.util.TimeConversion;
import com.sun.management.OperatingSystemMXBean;

public class CPUHarvester extends AbstractCPUSampler {
    private final OperatingSystemMXBean osMBean;

    public CPUHarvester() {
        osMBean = ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean());
    }

    protected double getProcessCpuTime() {
        return TimeConversion.convertNanosToSeconds(osMBean.getProcessCpuTime());
    }
}