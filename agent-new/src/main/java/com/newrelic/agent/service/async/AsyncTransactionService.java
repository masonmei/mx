package com.newrelic.agent.service.async;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.deps.com.google.common.cache.Cache;
import com.newrelic.deps.com.google.common.cache.CacheBuilder;
import com.newrelic.deps.com.google.common.cache.RemovalCause;
import com.newrelic.deps.com.google.common.cache.RemovalListener;
import com.newrelic.deps.com.google.common.cache.RemovalNotification;
import com.newrelic.deps.com.google.common.collect.Queues;
import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;

public class AsyncTransactionService extends AbstractService implements HarvestListener {
  private static final Queue<Map.Entry<Object, Transaction>> TIMED_OUT = Queues.newConcurrentLinkedQueue();

  private static final Map.Entry<Object, Transaction> NO_OP_MARKER = new Map.Entry<Object, Transaction>() {
    public Object getKey() {
      return null;
    }

    public Transaction getValue() {
      return null;
    }

    public Transaction setValue(Transaction value) {
      return null;
    }
  };

  private static final RemovalListener<Object, Transaction> removalListener =
          new RemovalListener<Object, Transaction>() {
            public void onRemoval(RemovalNotification<Object, Transaction> notification) {
              RemovalCause cause = notification.getCause();
              if (cause == RemovalCause.EXPLICIT) {
                Agent.LOG.log(Level.FINEST, "{2}: Key {0} with transaction {1} removed from cache.",
                                     new Object[] {notification.getKey(), notification.getValue(), cause});
              } else {
                Agent.LOG.log(Level.FINE,
                                     "{2}: The registered async activity with async context {0} has timed out"
                                             + " for " + "transaction {1} and been removed from the cache.",
                                     new Object[] {notification.getKey(), notification.getValue(), cause});

                AsyncTransactionService.TIMED_OUT.add(notification);
              }
            }
          };

  private static final Cache<Object, Transaction> PENDING_ACTIVITIES = makeCache(removalListener);

  public AsyncTransactionService() {
    super(AsyncTransactionService.class.getSimpleName());
  }

  private static final Cache<Object, Transaction> makeCache(RemovalListener<Object, Transaction> removalListener) {
    long timeoutSec = ((Integer) ServiceFactory.getConfigService().getDefaultAgentConfig()
                                         .getValue("async_timeout", Integer.valueOf(180))).intValue();
    return CacheBuilder.newBuilder().weakKeys().expireAfterWrite(timeoutSec, TimeUnit.SECONDS)
                   .removalListener(removalListener).build();
  }

  public void cleanUpPendingTransactions() {
    PENDING_ACTIVITIES.cleanUp();
    Agent.LOG.log(Level.FINER, "Cleaning up the pending activities cache.");
  }

  public boolean putIfAbsent(Object key, Transaction tx) {
    boolean result = false;
    synchronized(PENDING_ACTIVITIES) {
      if (PENDING_ACTIVITIES.getIfPresent(key) == null) {
        PENDING_ACTIVITIES.put(key, tx);
        result = true;
      }
    }
    return result;
  }

  public Transaction extractIfPresent(Object key) {
    Transaction result = null;
    synchronized(PENDING_ACTIVITIES) {
      result = (Transaction) PENDING_ACTIVITIES.getIfPresent(key);
      if (result != null) {
        PENDING_ACTIVITIES.invalidate(key);
      }
    }
    return result;
  }

  public void beforeHarvest(String appName, StatsEngine statsEngine) {
    cleanUpPendingTransactions();
    Map.Entry notification = (Map.Entry) TIMED_OUT.poll();
    if (notification != null) {
      Agent.LOG.log(Level.FINER, "Pulling async keys from timeout queue.");
      TIMED_OUT.add(NO_OP_MARKER);
      while (notification != NO_OP_MARKER) {
        ((Transaction) notification.getValue()).timeoutAsyncActivity(notification.getKey());
        Agent.LOG.log(Level.FINER, "Timed out key {0} in transaction {1}",
                             new Object[] {notification.getKey(), notification.getValue()});

        notification = (Map.Entry) TIMED_OUT.poll();
      }
    }
  }

  protected int queueSizeForTesting() {
    return TIMED_OUT.size();
  }

  public void afterHarvest(String appName) {
  }

  public boolean isEnabled() {
    return true;
  }

  protected void doStart() throws Exception {
    ServiceFactory.getHarvestService().addHarvestListener(this);
  }

  protected void doStop() throws Exception {
    ServiceFactory.getHarvestService().removeHarvestListener(this);
  }
}