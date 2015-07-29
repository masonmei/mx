package com.newrelic.agent.logging;

import com.newrelic.deps.ch.qos.logback.classic.spi.ILoggingEvent;
import com.newrelic.deps.ch.qos.logback.core.filter.Filter;
import com.newrelic.deps.ch.qos.logback.core.spi.FilterReply;
import com.newrelic.deps.org.slf4j.Marker;
import java.util.logging.Level;

class FineFilter extends Filter<ILoggingEvent>
{
  private static FineFilter instance;
  private volatile Level javaLevel;
  private final Marker markerToMatch = LogbackMarkers.FINE_MARKER;

  private final Marker markerToFail = LogbackMarkers.FINER_MARKER;

  public static FineFilter getFineFilter() {
    if (instance == null) {
      instance = new FineFilter();
    }
    return instance;
  }

  private FineFilter()
  {
    this.javaLevel = Level.INFO;
  }

  public FilterReply decide(ILoggingEvent pEvent)
  {
    if (!isStarted()) {
      return FilterReply.NEUTRAL;
    }

    if (Level.FINE.equals(this.javaLevel)) {
      Marker marker = pEvent.getMarker();
      if (marker == null)
        return FilterReply.NEUTRAL;
      if (marker.contains(this.markerToMatch))
        return FilterReply.ACCEPT;
      if (marker.contains(this.markerToFail)) {
        return FilterReply.DENY;
      }
    }

    return FilterReply.NEUTRAL;
  }

  public boolean isEnabledFor(Level pLevel)
  {
    return this.javaLevel.intValue() <= pLevel.intValue();
  }

  public void setLevel(Level level)
  {
    this.javaLevel = level;
  }

  public Level getLevel()
  {
    return this.javaLevel;
  }

  public void start()
  {
    if (this.javaLevel != null)
      super.start();
  }
}