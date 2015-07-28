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
package com.newrelic.deps.ch.qos.logback.core.pattern;

import com.newrelic.deps.ch.qos.logback.core.Context;
import com.newrelic.deps.ch.qos.logback.core.spi.ContextAware;

public class ConverterUtil {

  /**
   * Start converters in the chain of converters.
   *
   * @param head
   */
  public static void startConverters(Converter head) {
    Converter c = head;
    while (c != null) {
      // CompositeConverter is a subclass of  DynamicConverter
      if (c instanceof CompositeConverter) {
        CompositeConverter cc = (CompositeConverter) c;
        Converter childConverter = cc.childConverter;
        startConverters(childConverter);
        cc.start();
      } else if (c instanceof DynamicConverter) {
        DynamicConverter dc = (DynamicConverter) c;
        dc.start();
      }
      c = c.getNext();
    }
  }


  public static <E> Converter<E> findTail(Converter<E> head) {
    Converter<E> p = head;
    while (p != null) {
      Converter<E> next = p.getNext();
      if (next == null) {
        break;
      } else {
        p = next;
      }
    }
    return p;
  }

  public static <E> void setContextForConverters(Context context, Converter<E> head) {
    Converter c = head;
    while (c != null) {
      if (c instanceof ContextAware) {
        ((ContextAware) c).setContext(context);
      }
      c = c.getNext();
    }
  }
}
