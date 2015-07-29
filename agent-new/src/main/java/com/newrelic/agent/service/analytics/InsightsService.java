package com.newrelic.agent.service.analytics;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.Service;
import com.newrelic.api.agent.Insights;

public abstract interface InsightsService extends Service, Insights {
    public abstract Insights getTransactionInsights(AgentConfig paramAgentConfig);

    public abstract void storeEvent(String paramString, CustomInsightsEvent paramCustomInsightsEvent);
}