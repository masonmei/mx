package com.newrelic.agent.service.analytics;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.service.ServiceFactory;

abstract class CustomInsightsEventsConfigUtils
{
  public static final int DEFAULT_MAX_SAMPLES_STORED = 10000;
  public static final boolean DEFAULT_ENABLED = true;

  static boolean isCustomInsightsEventsEnabled(AgentConfig config, int maxSamplesStored)
  {
    boolean notHighSecurity = !ServiceFactory.getConfigService().getDefaultAgentConfig().isHighSecurity();
    boolean storedMoreThan0 = maxSamplesStored > 0;
    Boolean configEnabled = (Boolean)config.getValue("custom_insights_events.enabled", Boolean.valueOf(true));

    Boolean featureGateEnabled = (Boolean)config.getValue("custom_insights_events.collect_custom_events", Boolean.valueOf(true));
    return (notHighSecurity) && (storedMoreThan0) && (configEnabled.booleanValue()) && (featureGateEnabled.booleanValue());
  }

  static int getMaxSamplesStored(AgentConfig config) {
    return ((Integer)config.getValue("custom_insights_events.max_samples_stored", Integer.valueOf(10000))).intValue();
  }
}