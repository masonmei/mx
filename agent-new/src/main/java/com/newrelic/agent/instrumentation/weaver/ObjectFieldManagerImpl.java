package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.ObjectFieldManager;
import com.newrelic.deps.com.google.common.collect.MapMaker;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.agent.logging.IAgentLogger;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

class ObjectFieldManagerImpl
  implements ObjectFieldManager
{
  final ConcurrentMap<String, ConcurrentMap<Object, Object>> classObjectFields;

  public ObjectFieldManagerImpl()
  {
    this.classObjectFields = Maps.newConcurrentMap();
  }

  public Object getFieldContainer(String className, Object target)
  {
    Map map = (Map)this.classObjectFields.get(className);
    if (map != null) {
      return map.get(target);
    }

    return null;
  }

  public void initializeFields(String className, Object target, Object fieldContainer)
  {
    ConcurrentMap map = (ConcurrentMap)this.classObjectFields.get(className);
    if (map != null) {
      Object existing = map.putIfAbsent(target, fieldContainer);
      if (existing == null);
    }
  }

  public void createClassObjectFields(String className)
  {
    ConcurrentMap existing = (ConcurrentMap)this.classObjectFields.putIfAbsent(className, new MapMaker().weakKeys().concurrencyLevel(8).makeMap());

    if (existing != null)
      Agent.LOG.log(Level.FINEST, className, new Object[] { " already has an object field map" });
  }
}