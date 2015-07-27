package com.newrelic.agent;

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

import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsWork;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.transaction.MergeStatsEngineResolvingScope;

public class TransactionService extends AbstractService implements HarvestListener {
    private static final ThreadLocal<Boolean> NOTICE_REQUEST_THREAD = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    private static final ThreadLocal<Boolean> NOTICE_BACKGROUND_THREAD = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    private final List<TransactionListener> transactionListeners = new CopyOnWriteArrayList<TransactionListener>();
    private final Map<Long, Transaction> transactionThreadMap = new ConcurrentHashMap<Long, Transaction>();

    public TransactionService() {
        super(TransactionService.class.getSimpleName());
    }

    public static void noticeRequestThread(long threadId) {
        if (NOTICE_REQUEST_THREAD.get()) {
            return;
        }
        ServiceFactory.getThreadService().noticeRequestThread(threadId);
        NOTICE_REQUEST_THREAD.set(Boolean.TRUE);
    }

    public static void noticeBackgroundThread(long threadId) {
        if (NOTICE_BACKGROUND_THREAD.get()) {
            return;
        }
        ServiceFactory.getThreadService().noticeBackgroundThread(threadId);
        NOTICE_BACKGROUND_THREAD.set(Boolean.TRUE);
    }

    public void processTransaction(TransactionData transactionData, TransactionStats transactionStats) {
        try {
            doProcessTransaction(transactionData, transactionStats);
        } catch (Exception e) {
            String msg = MessageFormat.format("Error recording transaction \"{0}\": {1}", transactionData
                                                         .getBlameMetricName(),
                                                     e);

            if (getLogger().isLoggable(Level.FINER)) {
                getLogger().log(Level.FINER, msg, e);
            } else {
                getLogger().warning(msg);
            }
        }
    }

    private void doProcessTransaction(TransactionData transactionData, TransactionStats transactionStats) {
        if ((!ServiceFactory.getServiceManager().isStarted()) || (!ServiceFactory.getAgent().isEnabled())) {
            return;
        }

        if (Agent.isDebugEnabled()) {
            getLogger().finer("Recording metrics for " + transactionData);
        }

        String transactionSizeMetric = "Supportability/TransactionSize";
        boolean sizeLimitExceeded = transactionData.getAgentAttributes().get("size_limit") != null;
        transactionStats.getUnscopedStats().getStats(transactionSizeMetric)
                .recordDataPoint(transactionData.getTransactionSize());

        if (sizeLimitExceeded) {
            transactionStats.getUnscopedStats().getStats("Supportability/TransactionSizeClamp").incrementCallCount();
        }

        if (transactionData.isWebTransaction()) {
            noticeRequestThread(transactionData.getThreadId());
        } else {
            noticeBackgroundThread(transactionData.getThreadId());
        }
        if (transactionData.getDispatcher() != null) {
            for (TransactionListener listener : transactionListeners) {
                listener.dispatcherTransactionFinished(transactionData, transactionStats);
            }
        } else if (Agent.isDebugEnabled()) {
            getLogger().finer("Skipping transaction trace for " + transactionData);
        }

        StatsService statsService = ServiceFactory.getStatsService();
        StatsWork statsWork = new MergeStatsEngineResolvingScope(transactionData.getBlameMetricName(),
                                                                        transactionData.getApplicationName(),
                                                                        transactionStats);

        statsService.doStatsWork(statsWork);
    }

    protected void doStart() {
        ServiceFactory.getHarvestService().addHarvestListener(this);
    }

    protected void doStop() {
        transactionListeners.clear();
        transactionThreadMap.clear();
    }

    public void addTransaction(Transaction tx) {
        long id = Thread.currentThread().getId();
        transactionThreadMap.put(id, tx);
    }

    public void removeTransaction() {
        transactionThreadMap.remove(Thread.currentThread().getId());
    }

    public Set<Long> getRunningThreadIds() {
        Set<Long> runningThreadIds = new HashSet<Long>();
        for (Entry<Long, Transaction> entry : transactionThreadMap.entrySet()) {
            Transaction tx = entry.getValue();
            if (tx.isStarted()) {
                runningThreadIds.add(entry.getKey());
            }
        }
        return runningThreadIds;
    }

    public Set<Long> getThreadIds() {
        return new HashSet<Long>(transactionThreadMap.keySet());
    }

    public void addTransactionListener(TransactionListener listener) {
        transactionListeners.add(listener);
    }

    public void removeTransactionListener(TransactionListener listener) {
        transactionListeners.remove(listener);
    }

    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        Set threadIds = transactionThreadMap.keySet();
        Iterator it = threadIds.iterator();
        while (it.hasNext()) {
            long threadId = (Long) it.next();
            if (hasThreadTerminated(threadId)) {
                it.remove();
            }
        }
    }

    private boolean hasThreadTerminated(long threadId) {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo threadInfo = threadMXBean.getThreadInfo(threadId, 0);
        return threadInfo == null || threadInfo.getThreadState() == Thread.State.TERMINATED;
    }

    public void afterHarvest(String appName) {
    }

    public boolean isEnabled() {
        return true;
    }

    public Transaction getTransaction(boolean createIfNotExists) {
        return Transaction.getTransaction(createIfNotExists);
    }
}