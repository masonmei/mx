package com.newrelic.agent.service.analytics;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.Service;
import com.newrelic.api.agent.Insights;

public interface InsightsService extends Service, Insights {
    Insights getTransactionInsights(AgentConfig paramAgentConfig);

    void storeEvent(String paramString, CustomInsightsEvent paramCustomInsightsEvent);
}