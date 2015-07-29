package com.newrelic.agent.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ErrorCollectorConfigImpl extends BaseConfig implements ErrorCollectorConfig {
  public static final String ENABLED = "enabled";
  public static final String COLLECT_ERRORS = "collect_errors";
  public static final String IGNORE_STATUS_CODES = "ignore_status_codes";
  public static final String IGNORE_ERRORS = "ignore_errors";
  public static final String IGNORE_ERROR_PRIORITY_KEY = "error_collector.ignoreErrorPriority";
  public static final boolean DEFAULT_ENABLED = true;
  public static final boolean DEFAULT_COLLECT_ERRORS = true;
  public static final Set<Integer> DEFAULT_IGNORE_STATUS_CODES =
          Collections.unmodifiableSet(new HashSet(Arrays.asList(new Integer[] {Integer.valueOf(404)})));

  public static final Set<String> DEFAULT_IGNORE_ERRORS =
          Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[] {"akka.actor.ActorKilledException"})));
  public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.error_collector.";
  private final boolean isEnabled;
  private final Set<String> ignoreErrors;
  private final Set<Integer> ignoreStatusCodes;

  private ErrorCollectorConfigImpl(Map<String, Object> props) {
    super(props, "newrelic.config.error_collector.");
    isEnabled = initEnabled();
    ignoreErrors = initIgnoreErrors();
    ignoreStatusCodes =
            Collections.unmodifiableSet(getIntegerSet("ignore_status_codes", DEFAULT_IGNORE_STATUS_CODES));
  }

  static ErrorCollectorConfig createErrorCollectorConfig(Map<String, Object> settings) {
    if (settings == null) {
      settings = Collections.emptyMap();
    }
    return new ErrorCollectorConfigImpl(settings);
  }

  private Set<String> initIgnoreErrors() {
    Collection<String> uniqueErrors = getUniqueStrings("ignore_errors");
    uniqueErrors = getProperty("ignore_errors") == null ? DEFAULT_IGNORE_ERRORS : uniqueErrors;
    Set result = new HashSet(uniqueErrors.size());
    for (String uniqueError : uniqueErrors) {
      result.add(uniqueError.replace('/', '.'));
    }
    return Collections.unmodifiableSet(result);
  }

  private boolean initEnabled() {
    boolean isEnabled = ((Boolean) getProperty("enabled", Boolean.valueOf(true))).booleanValue();

    boolean canCollectErrors = ((Boolean) getProperty("collect_errors", Boolean.valueOf(true))).booleanValue();
    return (isEnabled) && (canCollectErrors);
  }

  public Set<String> getIgnoreErrors() {
    return ignoreErrors;
  }

  public Set<Integer> getIgnoreStatusCodes() {
    return ignoreStatusCodes;
  }

  public boolean isEnabled() {
    return isEnabled;
  }
}