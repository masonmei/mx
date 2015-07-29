package com.newrelic.agent;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import com.newrelic.agent.metric.MetricIdRegistry;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsEngineImpl;
import com.newrelic.agent.util.DefaultThreadFactory;
import com.newrelic.agent.util.SafeWrappers;

public class HarvestServiceImpl extends AbstractService implements HarvestService {
  public static final String HARVEST_THREAD_NAME = "New Relic Harvest Service";
  private static final long INITIAL_DELAY = 30000L;
  private static final long MIN_HARVEST_INTERVAL_IN_NANOSECONDS = TimeUnit.NANOSECONDS.convert(55L, TimeUnit.SECONDS);
  private static final long REPORTING_PERIOD_IN_MILLISECONDS = TimeUnit.MILLISECONDS.convert(60L, TimeUnit.SECONDS);
  private final ScheduledExecutorService scheduledExecutor;
  private final List<HarvestListener> harvestListeners = new CopyOnWriteArrayList();
  private final Map<IRPMService, HarvestTask> harvestTasks = new HashMap();

  public HarvestServiceImpl() {
    super(HarvestService.class.getSimpleName());
    ThreadFactory threadFactory = new DefaultThreadFactory("New Relic Harvest Service", true);
    scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
    ServiceFactory.getRPMServiceManager().addConnectionListener(new ConnectionListenerImpl());
  }

  public boolean isEnabled() {
    return true;
  }

  protected void doStart() {
  }

  public void startHarvest(IRPMService rpmService) {
    HarvestTask harvestTask = getOrCreateHarvestTask(rpmService);
    harvestTask.start();
  }

  private synchronized HarvestTask getOrCreateHarvestTask(IRPMService rpmService) {
    HarvestTask harvestTask = (HarvestTask) harvestTasks.get(rpmService);
    if (harvestTask == null) {
      harvestTask = new HarvestTask(rpmService);
      harvestTasks.put(rpmService, harvestTask);
    }
    return harvestTask;
  }

  private synchronized List<HarvestTask> getHarvestTasks() {
    return new ArrayList(harvestTasks.values());
  }

  public void addHarvestListener(HarvestListener listener) {
    harvestListeners.add(listener);
  }

  public void removeHarvestListener(HarvestListener listener) {
    harvestListeners.remove(listener);
  }

  protected void doStop() {
    List<HarvestTask> tasks = getHarvestTasks();
    for (HarvestTask task : tasks) {
      task.stop();
    }
    scheduledExecutor.shutdown();
  }

  private ScheduledFuture<?> scheduleHarvestTask(HarvestTask harvestTask) {
    return scheduledExecutor.scheduleAtFixedRate(SafeWrappers.safeRunnable(harvestTask), getInitialDelay(),
                                                        getReportingPeriod(), TimeUnit.MILLISECONDS);
  }

  public long getInitialDelay() {
    return 30000L;
  }

  public long getReportingPeriod() {
    return REPORTING_PERIOD_IN_MILLISECONDS;
  }

  public long getMinHarvestInterval() {
    return MIN_HARVEST_INTERVAL_IN_NANOSECONDS;
  }

  public void harvestNow() {
    List<HarvestTask> tasks = getHarvestTasks();
    for (HarvestTask task : tasks) {
      task.harvestNow();
    }
  }

  private void reportHarvest(String appName, StatsEngine statsEngine, IRPMService rpmService) {
    try {
      rpmService.harvest(statsEngine);
    } catch (Exception e) {
      String msg = MessageFormat.format("Error reporting harvest data for {0}: {1}", new Object[] {appName, e});
      if (getLogger().isLoggable(Level.FINER)) {
        getLogger().log(Level.FINER, msg, e);
      } else {
        getLogger().finer(msg);
      }
    }
  }

  private void notifyListenerBeforeHarvest(String appName, StatsEngine statsEngine, HarvestListener listener) {
    try {
      listener.beforeHarvest(appName, statsEngine);
    } catch (Throwable e) {
      String msg = MessageFormat.format("Error harvesting data for {0}: {1}", new Object[] {appName, e});
      if (getLogger().isLoggable(Level.FINER)) {
        getLogger().log(Level.FINER, msg, e);
      } else {
        getLogger().finer(msg);
      }
    }
  }

  private void notifyListenerAfterHarvest(String appName, HarvestListener listener) {
    try {
      listener.afterHarvest(appName);
    } catch (Throwable e) {
      String msg = MessageFormat.format("Error harvesting data for {0}: {1}", new Object[] {appName, e});
      if (getLogger().isLoggable(Level.FINER)) {
        getLogger().log(Level.FINER, msg, e);
      } else {
        getLogger().finer(msg);
      }
    }
  }

  private class ConnectionListenerImpl implements ConnectionListener {
    private ConnectionListenerImpl() {
    }

    public void connected(IRPMService rpmService, Map<String, Object> connectionInfo) {
      startHarvest(rpmService);
    }

    public void disconnected(IRPMService rpmService) {
    }
  }

  private final class HarvestTask implements Runnable {
    private final IRPMService rpmService;
    private final Lock harvestLock = new ReentrantLock();
    private ScheduledFuture<?> task;
    private StatsEngine lastStatsEngine = new StatsEngineImpl();
    private long lastHarvestStartTime;

    private HarvestTask(IRPMService rpmService) {
      this.rpmService = rpmService;
    }

    public void run() {
      try {
        if (shouldHarvest()) {
          harvest();
        }
      } catch (Throwable t) {
        String msg = MessageFormat.format("Unexpected exception during harvest: {0}", new Object[] {t});
        if (getLogger().isLoggable(Level.FINER)) {
          getLogger().log(Level.WARNING, msg, t);
        } else {
          getLogger().warning(msg);
        }
      }
    }

    private boolean shouldHarvest() {
      return System.nanoTime() - lastHarvestStartTime >= getMinHarvestInterval();
    }

    private synchronized void start() {
      if (!isRunning()) {
        stop();
        String msg = MessageFormat.format("Scheduling harvest task for {0}",
                                                 new Object[] {rpmService.getApplicationName()});
        getLogger().log(Level.FINE, msg);
        task = HarvestServiceImpl.this.scheduleHarvestTask(this);
      }
    }

    private synchronized void stop() {
      if (task != null) {
        getLogger().fine(MessageFormat.format("Cancelling harvest task for {0}",
                                                     new Object[] {rpmService.getApplicationName()}));

        task.cancel(false);
      }
    }

    private boolean isRunning() {
      if (task == null) {
        return false;
      }
      return (!task.isCancelled()) || (task.isDone());
    }

    private void harvestNow() {
      if (rpmService.isConnected()) {
        String msg = MessageFormat.format("Sending metrics for {0} immediately",
                                                 new Object[] {rpmService.getApplicationName()});

        getLogger().info(msg);
        harvest();
      }
    }

    private void harvest() {
      harvestLock.lock();
      try {
        doHarvest();
      } catch (ServerCommandException e) {
      } catch (IgnoreSilentlyException e) {
      } catch (Throwable e) {
        getLogger().log(Level.INFO, "Error sending metric data for {0}: {1}",
                               new Object[] {rpmService.getApplicationName(), e.toString()});
      } finally {
        harvestLock.unlock();
      }
    }

    private void doHarvest() throws Exception {
      lastHarvestStartTime = System.nanoTime();
      String appName = rpmService.getApplicationName();
      if (getLogger().isLoggable(Level.FINE)) {
        String msg = MessageFormat.format("Starting harvest for {0}", new Object[] {appName});
        getLogger().fine(msg);
      }
      StatsEngine harvestStatsEngine = ServiceFactory.getStatsService().getStatsEngineForHarvest(appName);
      harvestStatsEngine.mergeStats(lastStatsEngine);
      try {
        for (HarvestListener listener : harvestListeners) {
          HarvestServiceImpl.this.notifyListenerBeforeHarvest(appName, harvestStatsEngine, listener);
        }

        HarvestServiceImpl.this.reportHarvest(appName, harvestStatsEngine, rpmService);

        for (HarvestListener listener : harvestListeners) {
          HarvestServiceImpl.this.notifyListenerAfterHarvest(appName, listener);
        }
      } finally {
        if (harvestStatsEngine.getSize() > MetricIdRegistry.METRIC_LIMIT) {
          harvestStatsEngine.clear();
        }
        lastStatsEngine = harvestStatsEngine;
        long duration =
                TimeUnit.MILLISECONDS.convert(System.nanoTime() - lastHarvestStartTime, TimeUnit.NANOSECONDS);

        harvestStatsEngine.getResponseTimeStats("Supportability/Harvest")
                .recordResponseTime(duration, TimeUnit.MILLISECONDS);

        if (getLogger().isLoggable(Level.FINE)) {
          String msg = MessageFormat.format("Harvest for {0} took {1} milliseconds",
                                                   new Object[] {appName, Long.valueOf(duration)});
          getLogger().fine(msg);
        }
      }
    }
  }
}