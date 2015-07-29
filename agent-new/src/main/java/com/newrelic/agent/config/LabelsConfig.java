package com.newrelic.agent.config;

import java.util.Map;

public abstract interface LabelsConfig
{
  public abstract Map<String, String> getLabels();
}