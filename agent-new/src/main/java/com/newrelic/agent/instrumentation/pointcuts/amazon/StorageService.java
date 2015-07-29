package com.newrelic.agent.instrumentation.pointcuts.amazon;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName={"org/jets3t/service/StorageService"})
public abstract interface StorageService
{
  public abstract String getEndpoint();
}