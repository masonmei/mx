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
package com.newrelic.deps.ch.qos.logback.core.sift;

import java.util.ArrayList;
import java.util.List;

import com.newrelic.deps.ch.qos.logback.core.Appender;
import com.newrelic.deps.ch.qos.logback.core.Context;
import com.newrelic.deps.ch.qos.logback.core.joran.event.SaxEvent;
import com.newrelic.deps.ch.qos.logback.core.joran.spi.JoranException;

public abstract class AppenderFactoryBase<E> {

  final List<SaxEvent> eventList;
  
  protected AppenderFactoryBase(List<SaxEvent> eventList) {
    this.eventList = new ArrayList<SaxEvent>(eventList);
    removeSiftElement();
  }

  void removeSiftElement() {
    eventList.remove(0);
    eventList.remove(eventList.size() - 1);
  }

  public abstract SiftingJoranConfiguratorBase<E> getSiftingJoranConfigurator(String k);
  
  Appender<E> buildAppender(Context context, String discriminatingValue) throws JoranException {
    SiftingJoranConfiguratorBase<E> sjc = getSiftingJoranConfigurator(discriminatingValue);
    sjc.setContext(context);
    sjc.doConfigure(eventList);
    return sjc.getAppender();
  }

  public List<SaxEvent> getEventList() {
    return eventList;
  }

}
