package com.newrelic.agent.logging;

import java.util.logging.Level;

import com.newrelic.deps.ch.qos.logback.classic.spi.ILoggingEvent;
import com.newrelic.deps.ch.qos.logback.core.filter.Filter;
import com.newrelic.deps.ch.qos.logback.core.spi.FilterReply;
import com.newrelic.deps.org.slf4j.Marker;

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