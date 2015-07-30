package com.newrelic.agent.browser;

import com.newrelic.agent.service.Service;

public interface BrowserService extends Service {
    IBrowserConfig getBrowserConfig(String paramString);
}