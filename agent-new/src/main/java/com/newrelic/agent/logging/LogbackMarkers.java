package com.newrelic.agent.logging;

import com.newrelic.deps.org.slf4j.Marker;
import com.newrelic.deps.org.slf4j.MarkerFactory;

class LogbackMarkers {
    private static String FINE_STR = "FINE";
    public static final Marker FINE_MARKER = MarkerFactory.getMarker(FINE_STR);
    private static String FINER_STR = "FINER";
    public static final Marker FINER_MARKER = MarkerFactory.getMarker(FINER_STR);
    private static String FINEST_STR = "FINEST";
    public static final Marker FINEST_MARKER = MarkerFactory.getMarker(FINEST_STR);
}