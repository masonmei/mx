package com.newrelic.agent.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class ThreadIdLogbackConverter extends ClassicConverter {
    public String convert(ILoggingEvent event) {
        try {
            long theId = Thread.currentThread().getId();
            return Long.toString(theId);
        } catch (Exception e) {
        }
        return null;
    }
}