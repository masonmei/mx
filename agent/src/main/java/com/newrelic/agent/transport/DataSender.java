package com.newrelic.agent.transport;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.newrelic.agent.MetricData;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.profile.IProfile;
import com.newrelic.agent.service.analytics.CustomInsightsEvent;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.module.Jar;
import com.newrelic.agent.sql.SqlTrace;
import com.newrelic.agent.trace.TransactionTrace;

public abstract interface DataSender {
    public abstract Map<String, Object> connect(Map<String, Object> paramMap) throws Exception;

    public abstract List<List<?>> getAgentCommands() throws Exception;

    public abstract void queuePingCommand() throws Exception;

    public abstract void sendCommandResults(Map<Long, Object> paramMap) throws Exception;

    public abstract void sendErrorData(List<TracedError> paramList) throws Exception;

    public abstract void sendAnalyticsEvents(Collection<TransactionEvent> paramCollection) throws Exception;

    public abstract void sendCustomAnalyticsEvents(Collection<CustomInsightsEvent> paramCollection) throws Exception;

    public abstract List<List<?>> sendMetricData(long paramLong1, long paramLong2, List<MetricData> paramList)
            throws Exception;

    public abstract List<Long> sendProfileData(List<IProfile> paramList) throws Exception;

    public abstract void sendSqlTraceData(List<SqlTrace> paramList) throws Exception;

    public abstract void sendTransactionTraceData(List<TransactionTrace> paramList) throws Exception;

    public abstract void sendModules(List<Jar> paramList) throws Exception;

    public abstract void shutdown(long paramLong) throws Exception;

    public abstract List<?> getXRayParameters(Collection<Long> paramCollection) throws Exception;
}