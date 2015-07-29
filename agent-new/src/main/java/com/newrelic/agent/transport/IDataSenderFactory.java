package com.newrelic.agent.transport;

import com.newrelic.agent.config.AgentConfig;

public abstract interface IDataSenderFactory {
    public abstract DataSender create(AgentConfig paramAgentConfig);
}