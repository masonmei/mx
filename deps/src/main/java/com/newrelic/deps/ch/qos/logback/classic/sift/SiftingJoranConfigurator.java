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
package com.newrelic.deps.ch.qos.logback.classic.sift;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.newrelic.deps.ch.qos.logback.classic.spi.ILoggingEvent;
import com.newrelic.deps.ch.qos.logback.classic.util.DefaultNestedComponentRules;
import com.newrelic.deps.ch.qos.logback.core.Appender;
import com.newrelic.deps.ch.qos.logback.core.joran.action.ActionConst;
import com.newrelic.deps.ch.qos.logback.core.joran.action.AppenderAction;
import com.newrelic.deps.ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import com.newrelic.deps.ch.qos.logback.core.joran.spi.Pattern;
import com.newrelic.deps.ch.qos.logback.core.joran.spi.RuleStore;
import com.newrelic.deps.ch.qos.logback.core.sift.SiftingJoranConfiguratorBase;

public class SiftingJoranConfigurator  extends SiftingJoranConfiguratorBase<ILoggingEvent> {

  String key;
  String value;
  
  SiftingJoranConfigurator(String key, String value) {
    this.key = key;
    this.value = value;
  }
  
  @Override
  protected Pattern initialPattern() {
    return new Pattern("configuration");
  }
  
  @Override
  protected void addInstanceRules(RuleStore rs) {
    rs.addRule(new Pattern("configuration/appender"), new AppenderAction());
  }


  @Override
  protected void addDefaultNestedComponentRegistryRules(
      DefaultNestedComponentRegistry registry) {
    DefaultNestedComponentRules.addDefaultNestedComponentRegistryRules(registry);
  }
    
  @Override
  protected void buildInterpreter() {
    super.buildInterpreter();
    Map<String, Object> omap = interpreter.getInterpretationContext().getObjectMap();
    omap.put(ActionConst.APPENDER_BAG, new HashMap());
    omap.put(ActionConst.FILTER_CHAIN_BAG, new HashMap());
    Map<String, String> propertiesMap = new HashMap<String, String>();
    propertiesMap.put(key, value);
    interpreter.setInterpretationContextPropertiesMap(propertiesMap);
  }

  @SuppressWarnings("unchecked")
  public Appender<ILoggingEvent> getAppender() {
    Map<String, Object> omap = interpreter.getInterpretationContext().getObjectMap();
    HashMap appenderMap = (HashMap) omap.get(ActionConst.APPENDER_BAG);
    oneAndOnlyOneCheck(appenderMap);
    Collection values = appenderMap.values();
    if(values.size() == 0) {
      return null;
    }
    return (Appender<ILoggingEvent>) values.iterator().next();
  }
}
