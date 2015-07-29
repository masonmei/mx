package com.newrelic.agent.trace;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.service.ServiceFactory;

public class TransactionTraceBucket implements ITransactionSampler {
  private static final int NO_TRACE_LIMIT = 5;
  private final AtomicReference<TransactionData> expensiveTransaction = new AtomicReference();
  private final int topN;
  private final Lock readLock;
  private final Lock writeLock;
  private volatile Map<String, Long> tracedTransactions;
  private volatile long maxDurationInNanos;
  private int noTraceCount;

  public TransactionTraceBucket() {
    topN = ServiceFactory.getConfigService().getDefaultAgentConfig().getTransactionTracerConfig().getTopN();
    tracedTransactions = Collections.unmodifiableMap(new HashMap(topN));
    ReadWriteLock lock = new ReentrantReadWriteLock();
    readLock = lock.readLock();
    writeLock = lock.writeLock();
  }

  public boolean noticeTransaction(TransactionData td) {
    String msg;
    if (td.getDuration() <= td.getTransactionTracerConfig().getTransactionThresholdInNanos()) {
      if (Agent.LOG.isLoggable(Level.FINER)) {
        msg = MessageFormat.format("Transaction trace threshold not exceeded {0}", new Object[] {td});
        Agent.LOG.finer(msg);
      }
      return false;
    }
    if (td.getDuration() <= maxDurationInNanos) {
      return false;
    }
    readLock.lock();
    try {
      return noticeTransactionUnderLock(td);
    } finally {
      readLock.unlock();
    }
  }

  private boolean noticeTransactionUnderLock(TransactionData td) {
    Long lastDuration = (Long) tracedTransactions.get(td.getBlameMetricName());
    if ((lastDuration != null) && (td.getDuration() <= lastDuration.longValue())) {
      return false;
    }
    while (true) {
      TransactionData current = (TransactionData) expensiveTransaction.get();
      if ((current != null) && (current.getDuration() >= td.getDuration())) {
        return false;
      }
      if (expensiveTransaction.compareAndSet(current, td)) {
        maxDurationInNanos = td.getDuration();
        if (Agent.LOG.isLoggable(Level.FINER)) {
          String msg = MessageFormat.format("Captured expensive transaction trace for {0} {1}",
                                                   new Object[] {td.getApplicationName(), td});

          Agent.LOG.finer(msg);
        }
        return true;
      }
    }
  }

  public List<TransactionTrace> harvest(String appName) {
    TransactionData td = null;
    writeLock.lock();
    try {
      td = harvestUnderLock(appName);
    } finally {
      writeLock.unlock();
    }
    if (td == null) {
      return Collections.emptyList();
    }
    if (Agent.LOG.isLoggable(Level.FINER)) {
      String msg = MessageFormat.format("Sending transaction trace for {0} {1}",
                                               new Object[] {td.getApplicationName(), td});
      Agent.LOG.finer(msg);
    }
    TransactionTrace trace = TransactionTrace.getTransactionTrace(td);
    List<TransactionTrace> traces = new ArrayList<TransactionTrace>(1);
    traces.add(trace);
    return traces;
  }

  private TransactionData harvestUnderLock(String appName) {
    maxDurationInNanos = 0L;
    TransactionData td = (TransactionData) expensiveTransaction.getAndSet(null);
    noticeTracedTransaction(td);
    return td;
  }

  private void noticeTracedTransaction(TransactionData td) {
    if (topN == 0) {
      return;
    }
    int size = tracedTransactions.size();
    if (td == null) {
      noTraceCount += 1;
      if ((noTraceCount >= 5) && (size > 0)) {
        noTraceCount = 0;
        tracedTransactions = Collections.unmodifiableMap(new HashMap(topN));
      }
      return;
    }
    noTraceCount = 0;
    Map ttMap = new HashMap(topN);
    if (size < topN) {
      ttMap.putAll(tracedTransactions);
    }
    ttMap.put(td.getBlameMetricName(), Long.valueOf(td.getDuration()));
    tracedTransactions = Collections.unmodifiableMap(ttMap);
  }

  public void stop() {
    expensiveTransaction.set(null);
    tracedTransactions.clear();
  }

  public long getMaxDurationInNanos() {
    return maxDurationInNanos;
  }
}