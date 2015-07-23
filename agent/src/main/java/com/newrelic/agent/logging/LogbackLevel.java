package com.newrelic.agent.logging;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Marker;

enum LogbackLevel {
    OFF("off", ch.qos.logback.classic.Level.OFF, java.util.logging.Level.OFF, null),

    ALL("all", ch.qos.logback.classic.Level.ALL, java.util.logging.Level.ALL, null),

    FATAL("fatal", ch.qos.logback.classic.Level.ERROR, java.util.logging.Level.SEVERE, null),

    SEVERE("severe", ch.qos.logback.classic.Level.ERROR, java.util.logging.Level.SEVERE, null),

    ERROR("error", ch.qos.logback.classic.Level.ERROR, java.util.logging.Level.SEVERE, null),

    WARN("warn", ch.qos.logback.classic.Level.WARN, java.util.logging.Level.WARNING, null),

    WARNING("warning", ch.qos.logback.classic.Level.WARN, java.util.logging.Level.WARNING, null),

    INFO("info", ch.qos.logback.classic.Level.INFO, java.util.logging.Level.INFO, null),

    CONFIG("config", ch.qos.logback.classic.Level.INFO, java.util.logging.Level.CONFIG, null),

    FINE("fine", ch.qos.logback.classic.Level.DEBUG, java.util.logging.Level.FINE, LogbackMarkers.FINE_MARKER),

    FINER("finer", ch.qos.logback.classic.Level.DEBUG, java.util.logging.Level.FINER, LogbackMarkers.FINER_MARKER),

    FINEST("finest", ch.qos.logback.classic.Level.TRACE, java.util.logging.Level.FINEST, LogbackMarkers.FINEST_MARKER),

    DEBUG("debug", ch.qos.logback.classic.Level.DEBUG, java.util.logging.Level.FINE, null),

    TRACE("trace", ch.qos.logback.classic.Level.TRACE, java.util.logging.Level.FINEST, null);

    private static final Map<String, LogbackLevel> CONVERSION;
    private static final Map<java.util.logging.Level, LogbackLevel> JAVA_TO_LOGBACK;

    static {
        CONVERSION = new HashMap();

        JAVA_TO_LOGBACK = new HashMap();

        LogbackLevel[] levels = values();
        for (LogbackLevel level : levels) {
            CONVERSION.put(level.name, level);
        }

        JAVA_TO_LOGBACK.put(java.util.logging.Level.ALL, ALL);
        JAVA_TO_LOGBACK.put(java.util.logging.Level.FINER, FINER);
        JAVA_TO_LOGBACK.put(java.util.logging.Level.FINEST, FINEST);
        JAVA_TO_LOGBACK.put(java.util.logging.Level.FINE, FINE);
        JAVA_TO_LOGBACK.put(java.util.logging.Level.WARNING, WARNING);
        JAVA_TO_LOGBACK.put(java.util.logging.Level.SEVERE, SEVERE);
        JAVA_TO_LOGBACK.put(java.util.logging.Level.CONFIG, CONFIG);
        JAVA_TO_LOGBACK.put(java.util.logging.Level.INFO, INFO);
        JAVA_TO_LOGBACK.put(java.util.logging.Level.OFF, OFF);
    }

    private final String name;
    private final ch.qos.logback.classic.Level logbackLevel;
    private final java.util.logging.Level javaLevel;
    private final Marker marker;

    private LogbackLevel(String pName, ch.qos.logback.classic.Level pLogbackLevel, java.util.logging.Level pJavaLevel,
                         Marker pMarker) {
        name = pName;
        logbackLevel = pLogbackLevel;
        javaLevel = pJavaLevel;
        marker = pMarker;
    }

    public static LogbackLevel getLevel(String pName, LogbackLevel pDefault) {
        LogbackLevel level = (LogbackLevel) CONVERSION.get(pName);
        return level == null ? pDefault : level;
    }

    public static LogbackLevel getLevel(java.util.logging.Level pName) {
        return (LogbackLevel) JAVA_TO_LOGBACK.get(pName);
    }

    public Marker getMarker() {
        return marker;
    }

    public ch.qos.logback.classic.Level getLogbackLevel() {
        return logbackLevel;
    }

    public java.util.logging.Level getJavaLevel() {
        return javaLevel;
    }
}