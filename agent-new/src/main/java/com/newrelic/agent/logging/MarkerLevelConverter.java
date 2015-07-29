package com.newrelic.agent.logging;

import com.newrelic.deps.ch.qos.logback.classic.Level;
import com.newrelic.deps.ch.qos.logback.classic.pattern.ClassicConverter;
import com.newrelic.deps.ch.qos.logback.classic.spi.ILoggingEvent;
import com.newrelic.deps.org.slf4j.Marker;

public class MarkerLevelConverter extends ClassicConverter
{
  public String convert(ILoggingEvent pEvent)
  {
    Marker marker = pEvent.getMarker();
    if (marker == null) {
      return pEvent.getLevel().toString();
    }
    return marker.getName();
  }
}