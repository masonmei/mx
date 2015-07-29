package com.newrelic.agent.browser;

import com.newrelic.agent.service.Service;

public abstract interface BrowserService extends Service
{
  public abstract IBrowserConfig getBrowserConfig(String paramString);
}