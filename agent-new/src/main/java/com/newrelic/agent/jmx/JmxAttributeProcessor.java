package com.newrelic.agent.jmx;

import com.newrelic.agent.stats.StatsEngine;
import java.util.Map;
import javax.management.Attribute;
import javax.management.ObjectInstance;

public abstract interface JmxAttributeProcessor
{
  public abstract boolean process(StatsEngine paramStatsEngine, ObjectInstance paramObjectInstance,
                                  Attribute paramAttribute, String paramString, Map<String, Float> paramMap);
}