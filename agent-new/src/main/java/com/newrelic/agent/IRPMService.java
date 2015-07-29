package com.newrelic.agent;

import com.newrelic.agent.errors.ErrorService;
import com.newrelic.agent.profile.IProfile;
import com.newrelic.agent.service.Service;
import com.newrelic.agent.service.analytics.CustomInsightsEvent;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.module.Jar;
import com.newrelic.agent.sql.SqlTrace;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.trace.TransactionTrace;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract interface IRPMService extends Service
{
  public abstract List<Long> sendProfileData(List<IProfile> paramList)
    throws Exception;

  public abstract boolean isConnected();

  public abstract Map<String, Object> launch()
    throws Exception;

  public abstract String getHostString();

  public abstract void harvest(StatsEngine paramStatsEngine)
    throws Exception;

  public abstract List<List<?>> getAgentCommands()
    throws Exception;

  public abstract void sendCommandResults(Map<Long, Object> paramMap)
    throws Exception;

  public abstract void sendSqlTraceData(List<SqlTrace> paramList)
    throws Exception;

  public abstract void sendTransactionTraceData(List<TransactionTrace> paramList)
    throws Exception;

  public abstract String getApplicationName();

  public abstract void reconnect();

  public abstract ErrorService getErrorService();

  public abstract boolean isMainApp();

  public abstract boolean hasEverConnected();

  public abstract String getTransactionNamingScheme();

  public abstract long getConnectionTimestamp();

  public abstract void sendModules(List<Jar> paramList)
    throws Exception;

  public abstract void sendAnalyticsEvents(Collection<TransactionEvent> paramCollection)
    throws Exception;

  public abstract void sendCustomAnalyticsEvents(Collection<CustomInsightsEvent> paramCollection)
    throws Exception;

  public abstract Collection<?> getXRaySessionInfo(Collection<Long> paramCollection)
    throws Exception;
}