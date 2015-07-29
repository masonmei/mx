package com.newrelic.agent;

import java.util.Map;

import com.newrelic.agent.browser.BrowserTransactionState;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.CrossProcessConfig;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.transaction.PriorityTransactionName;

public abstract interface ITransaction {
    public abstract Dispatcher getDispatcher();

    public abstract Tracer getRootTracer();

    public abstract boolean isStarted();

    public abstract boolean isFinished();

    public abstract boolean isInProgress();

    public abstract boolean isIgnore();

    public abstract String getApplicationName();

    public abstract PriorityTransactionName getPriorityTransactionName();

    public abstract long getExternalTime();

    public abstract String getGuid();

    public abstract long getRunningDurationInNanos();

    public abstract AgentConfig getAgentConfig();

    public abstract CrossProcessConfig getCrossProcessConfig();

    public abstract void freezeTransactionName();

    public abstract BrowserTransactionState getBrowserTransactionState();

    public abstract InboundHeaderState getInboundHeaderState();

    public abstract CrossProcessTransactionState getCrossProcessTransactionState();

    public abstract TransactionActivity getTransactionActivity();

    public abstract Map<String, Object> getUserAttributes();

    public abstract Map<String, Map<String, String>> getPrefixedAgentAttributes();

    public abstract Map<String, Object> getAgentAttributes();

    public abstract Map<String, Object> getIntrinsicAttributes();
}