package com.newrelic.agent.browser;

public abstract interface IBrowserConfig
{
  public abstract String getBrowserTimingHeader();

  public abstract String getBrowserTimingFooter(BrowserTransactionState paramBrowserTransactionState);
}