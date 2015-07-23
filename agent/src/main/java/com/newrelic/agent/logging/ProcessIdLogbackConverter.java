package com.newrelic.agent.logging;

import java.lang.management.ManagementFactory;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class ProcessIdLogbackConverter extends ClassicConverter {
    private final String pid;

    public ProcessIdLogbackConverter() {
        pid = Integer.toString(getProcessId());
    }

    private static int getProcessId() {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        String[] split = runtimeName.split("@");
        if (split.length > 1) {
            return Integer.parseInt(split[0]);
        }
        return 0;
    }

    public String convert(ILoggingEvent event) {
        try {
            return pid;
        } catch (Exception e) {
        }
        return null;
    }
}