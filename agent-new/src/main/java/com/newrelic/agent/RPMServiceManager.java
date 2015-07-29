package com.newrelic.agent;

import com.newrelic.agent.application.PriorityApplicationName;
import com.newrelic.agent.service.Service;
import java.util.List;

public abstract interface RPMServiceManager extends Service
{
  public abstract void addConnectionListener(ConnectionListener paramConnectionListener);

  public abstract void removeConnectionListener(ConnectionListener paramConnectionListener);

  public abstract IRPMService getRPMService();

  public abstract IRPMService getRPMService(String paramString);

  public abstract IRPMService getOrCreateRPMService(String paramString);

  public abstract IRPMService getOrCreateRPMService(PriorityApplicationName paramPriorityApplicationName);

  public abstract List<IRPMService> getRPMServices();
}