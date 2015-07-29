package com.newrelic.agent.logging;

import com.newrelic.deps.org.slf4j.Marker;
import java.util.HashMap;
import java.util.Map;

 enum LogbackLevel
{
  OFF("off", com.newrelic.deps.ch.qos.logback.classic.Level.OFF, java.util.logging.Level.OFF, null),

  ALL("all", com.newrelic.deps.ch.qos.logback.classic.Level.ALL, java.util.logging.Level.ALL, null),

  FATAL("fatal", com.newrelic.deps.ch.qos.logback.classic.Level.ERROR, java.util.logging.Level.SEVERE, null),

  SEVERE("severe", com.newrelic.deps.ch.qos.logback.classic.Level.ERROR, java.util.logging.Level.SEVERE, null),

  ERROR("error", com.newrelic.deps.ch.qos.logback.classic.Level.ERROR, java.util.logging.Level.SEVERE, null),

  WARN("warn", com.newrelic.deps.ch.qos.logback.classic.Level.WARN, java.util.logging.Level.WARNING, null),

  WARNING("warning", com.newrelic.deps.ch.qos.logback.classic.Level.WARN, java.util.logging.Level.WARNING, null),

  INFO("info", com.newrelic.deps.ch.qos.logback.classic.Level.INFO, java.util.logging.Level.INFO, null),

  CONFIG("config", com.newrelic.deps.ch.qos.logback.classic.Level.INFO, java.util.logging.Level.CONFIG, null),

  FINE("fine", com.newrelic.deps.ch.qos.logback.classic.Level.DEBUG, java.util.logging.Level.FINE, LogbackMarkers.FINE_MARKER),

  FINER("finer", com.newrelic.deps.ch.qos.logback.classic.Level.DEBUG, java.util.logging.Level.FINER, LogbackMarkers.FINER_MARKER),

  FINEST("finest", com.newrelic.deps.ch.qos.logback.classic.Level.TRACE, java.util.logging.Level.FINEST, LogbackMarkers.FINEST_MARKER),

  DEBUG("debug", com.newrelic.deps.ch.qos.logback.classic.Level.DEBUG, java.util.logging.Level.FINE, null),

  TRACE("trace", com.newrelic.deps.ch.qos.logback.classic.Level.TRACE, java.util.logging.Level.FINEST, null);

  private final String name;
  private final com.newrelic.deps.ch.qos.logback.classic.Level logbackLevel;
  private final java.util.logging.Level javaLevel;
  private final Marker marker;
  private static final Map<String, LogbackLevel> CONVERSION;
  private static final Map<java.util.logging.Level, LogbackLevel> JAVA_TO_LOGBACK;

  private LogbackLevel(String pName, com.newrelic.deps.ch.qos.logback.classic.Level pLogbackLevel, java.util.logging.Level pJavaLevel, Marker pMarker)
  {
    this.name = pName;
    this.logbackLevel = pLogbackLevel;
    this.javaLevel = pJavaLevel;
    this.marker = pMarker;
  }

  public Marker getMarker()
  {
    return this.marker;
  }

  public com.newrelic.deps.ch.qos.logback.classic.Level getLogbackLevel()
  {
    return this.logbackLevel;
  }

  public java.util.logging.Level getJavaLevel()
  {
    return this.javaLevel;
  }

  public static LogbackLevel getLevel(String pName, LogbackLevel pDefault)
  {
    LogbackLevel level = (LogbackLevel)CONVERSION.get(pName);
    return level == null ? pDefault : level;
  }

  public static LogbackLevel getLevel(java.util.logging.Level pName)
  {
    return (LogbackLevel)JAVA_TO_LOGBACK.get(pName);
  }

  static
  {
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
}