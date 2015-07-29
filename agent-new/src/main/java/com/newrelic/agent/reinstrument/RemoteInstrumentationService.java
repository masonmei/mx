package com.newrelic.agent.reinstrument;

import com.newrelic.agent.service.Service;

public abstract interface RemoteInstrumentationService extends Service
{
  public abstract ReinstrumentResult processXml(String paramString);
}