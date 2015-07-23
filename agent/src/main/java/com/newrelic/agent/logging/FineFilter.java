package com.newrelic.agent.logging;

import java.util.logging.Level;

import org.slf4j.Marker;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

class FineFilter extends Filter<ILoggingEvent> {
    private static FineFilter instance;
    private final Marker markerToMatch = LogbackMarkers.FINE_MARKER;
    private final Marker markerToFail = LogbackMarkers.FINER_MARKER;
    private volatile Level javaLevel;

    private FineFilter() {
        javaLevel = Level.INFO;
    }

    public static FineFilter getFineFilter() {
        if (instance == null) {
            instance = new FineFilter();
        }
        return instance;
    }

    public FilterReply decide(ILoggingEvent pEvent) {
        if (!isStarted()) {
            return FilterReply.NEUTRAL;
        }

        if (Level.FINE.equals(javaLevel)) {
            Marker marker = pEvent.getMarker();
            if (marker == null) {
                return FilterReply.NEUTRAL;
            }
            if (marker.contains(markerToMatch)) {
                return FilterReply.ACCEPT;
            }
            if (marker.contains(markerToFail)) {
                return FilterReply.DENY;
            }
        }

        return FilterReply.NEUTRAL;
    }

    public boolean isEnabledFor(Level pLevel) {
        return javaLevel.intValue() <= pLevel.intValue();
    }

    public Level getLevel() {
        return javaLevel;
    }

    public void setLevel(Level level) {
        javaLevel = level;
    }

    public void start() {
        if (javaLevel != null) {
            super.start();
        }
    }
}