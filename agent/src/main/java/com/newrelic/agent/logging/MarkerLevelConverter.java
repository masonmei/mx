package com.newrelic.agent.logging;

import org.slf4j.Marker;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class MarkerLevelConverter extends ClassicConverter {
    public String convert(ILoggingEvent pEvent) {
        Marker marker = pEvent.getMarker();
        if (marker == null) {
            return pEvent.getLevel().toString();
        }
        return marker.getName();
    }
}