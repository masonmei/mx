package com.newrelic.agent.sql;

import com.newrelic.agent.service.Service;

public abstract interface SqlTraceService extends Service
{
  public abstract SqlTracerListener getSqlTracerListener(String paramString);
}