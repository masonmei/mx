/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2011, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package com.newrelic.deps.ch.qos.logback.classic.turbo;

import com.newrelic.deps.org.slf4j.Marker;
import com.newrelic.deps.org.slf4j.MarkerFactory;

import com.newrelic.deps.ch.qos.logback.classic.Level;
import com.newrelic.deps.ch.qos.logback.classic.Logger;
import com.newrelic.deps.ch.qos.logback.core.spi.FilterReply;

/**
 * Checks whether the marker in the event matches the marker specified by the 
 * user.
 */
public class MarkerFilter extends MatchingFilter {

  Marker markerToMatch;

  @Override
  public void start() {
    if(markerToMatch != null) {
      super.start();
    } else {
      addError("The marker property must be set for ["+getName()+"]");
    }
  }
  
  @Override
  public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
    if(!isStarted()) {
      return FilterReply.NEUTRAL;
    }
    
    if(marker == null) {
      return onMismatch;
    } 
    
    if(marker.contains(markerToMatch)) {
      return onMatch;
    } else {
      return onMismatch;
    }
  }

  /**
   * The marker to match in the event.
   * 
   * @param markerStr
   */
  public void setMarker(String markerStr) {
    if(markerStr != null) {
      this.markerToMatch = MarkerFactory.getMarker(markerStr);
    }
  }
}
