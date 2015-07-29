package com.newrelic.agent.config;

import java.util.Collections;
import java.util.Map;

final class ThreadProfilerConfigImpl extends BaseConfig
  implements ThreadProfilerConfig
{
  public static final String ENABLED = "enabled";
  public static final boolean DEFAULT_ENABLED = true;
  public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.thread_profiler.";
  private final boolean isEnabled;

  private ThreadProfilerConfigImpl(Map<String, Object> props)
  {
    super(props, "newrelic.config.thread_profiler.");
    this.isEnabled = ((Boolean)getProperty("enabled", Boolean.valueOf(true))).booleanValue();
  }

  public boolean isEnabled()
  {
    return this.isEnabled;
  }

  static ThreadProfilerConfig createThreadProfilerConfig(Map<String, Object> settings) {
    if (settings == null) {
      settings = Collections.emptyMap();
    }
    return new ThreadProfilerConfigImpl(settings);
  }
}