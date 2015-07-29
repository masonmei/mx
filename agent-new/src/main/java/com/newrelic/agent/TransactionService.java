package com.newrelic.agent;

import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.stats.SimpleStatsEngine;
import com.newrelic.agent.stats.Stats;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsWork;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.transaction.MergeStatsEngineResolvingScope;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public class TransactionService extends AbstractService
  implements HarvestListener
{
  private static final ThreadLocal<Boolean> NOTICE_REQUEST_THREAD = new ThreadLocal()
  {
    protected Boolean initialValue() {
      return Boolean.FALSE;
    }
  };

  private static final ThreadLocal<Boolean> NOTICE_BACKGROUND_THREAD = new ThreadLocal()
  {
    protected Boolean initialValue() {
      return Boolean.FALSE;
    }
  };

  private final List<TransactionListener> transactionListeners = new CopyOnWriteArrayList();
  private final Map<Long, Transaction> transactionThreadMap = new ConcurrentHashMap();

  public TransactionService() {
    super(TransactionService.class.getSimpleName());
  }

  public static void noticeRequestThread(long threadId) {
    if (((Boolean)NOTICE_REQUEST_THREAD.get()).booleanValue()) {
      return;
    }
    ServiceFactory.getThreadService().noticeRequestThread(Long.valueOf(threadId));
    NOTICE_REQUEST_THREAD.set(Boolean.TRUE);
  }

  public static void noticeBackgroundThread(long threadId) {
    if (((Boolean)NOTICE_BACKGROUND_THREAD.get()).booleanValue()) {
      return;
    }
    ServiceFactory.getThreadService().noticeBackgroundThread(Long.valueOf(threadId));
    NOTICE_BACKGROUND_THREAD.set(Boolean.TRUE);
  }

  public void processTransaction(TransactionData transactionData, TransactionStats transactionStats)
  {
    try
    {
      doProcessTransaction(transactionData, transactionStats);
    } catch (Exception e) {
      String msg = MessageFormat.format("Error recording transaction \"{0}\": {1}", new Object[] { transactionData.getBlameMetricName(), e });

      if (getLogger().isLoggable(Level.FINER))
        getLogger().log(Level.FINER, msg, e);
      else
        getLogger().warning(msg);
    }
  }

  private void doProcessTransaction(TransactionData transactionData, TransactionStats transactionStats)
  {
    if ((!ServiceFactory.getServiceManager().isStarted()) || (!ServiceFactory.getAgent().isEnabled())) {
      return;
    }

    if (Agent.isDebugEnabled()) {
      getLogger().finer("Recording metrics for " + transactionData);
    }

    String transactionSizeMetric = "Supportability/TransactionSize";
    boolean sizeLimitExceeded = transactionData.getAgentAttributes().get("size_limit") != null;
    transactionStats.getUnscopedStats().getStats(transactionSizeMetric).recordDataPoint(transactionData.getTransactionSize());

    if (sizeLimitExceeded) {
      transactionStats.getUnscopedStats().getStats("Supportability/TransactionSizeClamp").incrementCallCount();
    }

    if (transactionData.isWebTransaction())
      noticeRequestThread(transactionData.getThreadId());
    else {
      noticeBackgroundThread(transactionData.getThreadId());
    }
    if (transactionData.getDispatcher() != null) {
      for (TransactionListener listener : this.transactionListeners) {
        listener.dispatcherTransactionFinished(transactionData, transactionStats);
      }
    }
    else if (Agent.isDebugEnabled()) {
      getLogger().finer("Skipping transaction trace for " + transactionData);
    }

    StatsService statsService = ServiceFactory.getStatsService();
    StatsWork statsWork = new MergeStatsEngineResolvingScope(transactionData.getBlameMetricName(), transactionData.getApplicationName(), transactionStats);

    statsService.doStatsWork(statsWork);
  }

  protected void doStart()
  {
    ServiceFactory.getHarvestService().addHarvestListener(this);
  }

  protected void doStop()
  {
    this.transactionListeners.clear();
    this.transactionThreadMap.clear();
  }

  public void addTransaction(Transaction tx) {
    long id = Thread.currentThread().getId();
    this.transactionThreadMap.put(Long.valueOf(id), tx);
  }

  public void removeTransaction() {
    this.transactionThreadMap.remove(Long.valueOf(Thread.currentThread().getId()));
  }

  public Set<Long> getRunningThreadIds()
  {
    Set runningThreadIds = new HashSet();
    for (Entry entry : this.transactionThreadMap.entrySet()) {
      Transaction tx = (Transaction)entry.getValue();
      if (tx.isStarted()) {
        runningThreadIds.add(entry.getKey());
      }
    }
    return runningThreadIds;
  }

  public Set<Long> getThreadIds()
  {
    return new HashSet(this.transactionThreadMap.keySet());
  }

  public void addTransactionListener(TransactionListener listener) {
    this.transactionListeners.add(listener);
  }

  public void removeTransactionListener(TransactionListener listener) {
    this.transactionListeners.remove(listener);
  }

  public void beforeHarvest(String appName, StatsEngine statsEngine)
  {
    Set threadIds = this.transactionThreadMap.keySet();
    Iterator it = threadIds.iterator();
    while (it.hasNext()) {
      long threadId = ((Long)it.next()).longValue();
      if (hasThreadTerminated(threadId))
        it.remove();
    }
  }

  private boolean hasThreadTerminated(long threadId)
  {
    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    ThreadInfo threadInfo = threadMXBean.getThreadInfo(threadId, 0);
    if (threadInfo == null) {
      return true;
    }
    return threadInfo.getThreadState() == Thread.State.TERMINATED;
  }

  public void afterHarvest(String appName)
  {
  }

  public boolean isEnabled()
  {
    return true;
  }

  public Transaction getTransaction(boolean createIfNotExists) {
    return Transaction.getTransaction(createIfNotExists);
  }
}