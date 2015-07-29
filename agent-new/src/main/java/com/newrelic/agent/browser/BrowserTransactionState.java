package com.newrelic.agent.browser;

import java.util.Map;

public abstract interface BrowserTransactionState
{
  public abstract long getDurationInMilliseconds();

  public abstract long getExternalTimeInMilliseconds();

  public abstract String getBrowserTimingHeader();

  public abstract String getBrowserTimingHeaderForJsp();

  public abstract String getBrowserTimingFooter();

  public abstract String getTransactionName();

  public abstract Map<String, Object> getUserAttributes();

  public abstract Map<String, Object> getAgentAttributes();

  public abstract String getAppName();
}