package com.newrelic.agent.cache;

import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.util.MethodCache;
import com.newrelic.agent.util.SingleClassLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class CacheService extends AbstractService
  implements HarvestListener
{
  private static final long CLEAR_CACHE_INTERVAL = TimeUnit.NANOSECONDS.convert(600L, TimeUnit.SECONDS);

  private final ConcurrentMap<String, SingleClassLoader> singleClassLoaders = new ConcurrentHashMap();
  private final ConcurrentMap<ClassMethodSignature, MethodCache> methodCaches = new ConcurrentHashMap();
  private final String defaultAppName;
  private volatile long lastTimeCacheCleared = System.nanoTime();

  public CacheService() {
    super(CacheService.class.getSimpleName());
    this.defaultAppName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
  }

  protected void doStart() throws Exception
  {
    ServiceFactory.getHarvestService().addHarvestListener(this);
  }

  protected void doStop() throws Exception
  {
    ServiceFactory.getHarvestService().removeHarvestListener(this);
  }

  public boolean isEnabled()
  {
    return true;
  }

  public void afterHarvest(String appName)
  {
    if (!appName.equals(this.defaultAppName)) {
      return;
    }
    long timeNow = System.nanoTime();
    if (timeNow - this.lastTimeCacheCleared < CLEAR_CACHE_INTERVAL)
      return;
    try
    {
      clearCaches();
    } finally {
      this.lastTimeCacheCleared = timeNow;
    }
  }

  private void clearCaches() {
    for (SingleClassLoader singleClassLoader : this.singleClassLoaders.values()) {
      singleClassLoader.clear();
    }
    for (MethodCache methodCache : this.methodCaches.values())
      methodCache.clear();
  }

  public void beforeHarvest(String appName, StatsEngine statsEngine)
  {
  }

  public SingleClassLoader getSingleClassLoader(String className)
  {
    SingleClassLoader singleClassLoader = (SingleClassLoader)this.singleClassLoaders.get(className);
    if (singleClassLoader != null) {
      return singleClassLoader;
    }
    singleClassLoader = new SingleClassLoader(className);
    SingleClassLoader oldSingleClassLoader = (SingleClassLoader)this.singleClassLoaders.putIfAbsent(className, singleClassLoader);
    return oldSingleClassLoader == null ? singleClassLoader : oldSingleClassLoader;
  }

  public MethodCache getMethodCache(String className, String methodName, String methodDesc) {
    ClassMethodSignature key = new ClassMethodSignature(className.replace('/', '.'), methodName, methodDesc);
    MethodCache methodCache = (MethodCache)this.methodCaches.get(key);
    if (methodCache != null) {
      return methodCache;
    }
    methodCache = new MethodCache(methodName, new Class[0]);
    MethodCache oldMethodCache = (MethodCache)this.methodCaches.putIfAbsent(key, methodCache);
    return oldMethodCache == null ? methodCache : oldMethodCache;
  }
}