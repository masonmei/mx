package com.newrelic.agent.profile;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class ProfilingTaskControllerFactory {
    public static ProfilingTaskController createProfilingTaskController(ProfilingTask profilingTask) {
        if (isThreadCpuTimeSupportedAndEnabled()) {
            return new XrayCpuTimeController(profilingTask);
        }
        return new XrayClockTimeController(profilingTask);
    }

    private static boolean isThreadCpuTimeSupportedAndEnabled() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        return (threadMXBean.isThreadCpuTimeSupported()) && (threadMXBean.isThreadCpuTimeEnabled());
    }
}