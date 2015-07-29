package com.newrelic.agent.profile;

import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.deps.org.json.simple.JSONStreamAware;

public class KeyTransactionProfile implements IProfile, TransactionListener, JSONStreamAware {
    private static final int CAPACITY = 100;
    private static final long THREAD_CHECK_INTERVAL_IN_NANOS = TimeUnit.NANOSECONDS.convert(300L, TimeUnit.SECONDS);
    private final IProfile delegate;
    private final String keyTransaction;
    private final Map<Long, Queue<StackTraceHolder>> pendingStackTraces = new ConcurrentHashMap();
    private final Queue<StackTraceHolder> releasedStackTraces = new ConcurrentLinkedQueue();
    private final Set<Long> requestThreads = new HashSet();
    private long lastThreadCheck = System.nanoTime();

    public KeyTransactionProfile(ProfilerParameters parameters) {
        keyTransaction = parameters.getKeyTransaction();
        delegate = new Profile(parameters);
    }

    IProfile getDelegate() {
        return delegate;
    }

    public void start() {
        ServiceFactory.getTransactionService().addTransactionListener(this);
        delegate.start();
    }

    public void end() {
        ServiceFactory.getTransactionService().removeTransactionListener(this);
        pendingStackTraces.clear();
        releaseStackTraces();
        delegate.end();
    }

    public ProfilerParameters getProfilerParameters() {
        return delegate.getProfilerParameters();
    }

    public int getSampleCount() {
        return delegate.getSampleCount();
    }

    public Long getProfileId() {
        return delegate.getProfileId();
    }

    public ProfileTree getProfileTree(ThreadType threadType) {
        return delegate.getProfileTree(threadType);
    }

    public void writeJSONString(Writer out) throws IOException {
        delegate.writeJSONString(out);
    }

    public int trimBy(int count) {
        return delegate.trimBy(count);
    }

    public long getStartTimeMillis() {
        return delegate.getStartTimeMillis();
    }

    public long getEndTimeMillis() {
        return delegate.getEndTimeMillis();
    }

    private void releaseStackTraces() {
        while (true) {
            StackTraceHolder holder = (StackTraceHolder) releasedStackTraces.poll();
            if (holder == null) {
                return;
            }
            delegate.addStackTrace(holder.getThreadId(), holder.isRunnable(), holder.getType(), holder.getStackTrace());
        }
    }

    public void dispatcherTransactionFinished(TransactionData td, TransactionStats stats) {
        try {
            doDispatcherTransactionFinished(td, stats);
        } catch (Exception e) {
            String msg = MessageFormat.format("Error releasing stack traces for \"{0}\": {1}",
                                                     new Object[] {td.getBlameMetricName(), e});

            if (Agent.LOG.isLoggable(Level.FINEST)) {
                Agent.LOG.log(Level.FINEST, msg, e);
            } else {
                Agent.LOG.finer(msg);
            }
        }
    }

    private void doDispatcherTransactionFinished(TransactionData td, TransactionStats stats) {
        Queue holderQueue = getHolderQueue(td.getThreadId());
        if (holderQueue == null) {
            return;
        }
        boolean isKeyTransaction = isKeyTransaction(td);
        while (true) {
            StackTraceHolder holder = (StackTraceHolder) holderQueue.poll();
            if (holder == null) {
                break;
            }
            if (td.getStartTimeInNanos() <= holder.getStackTraceTime()) {
                if (td.getEndTimeInNanos() < holder.getStackTraceTime()) {
                    break;
                }
                if (isKeyTransaction) {
                    releasedStackTraces.add(holder);
                }
            }
        }
    }

    private boolean isKeyTransaction(TransactionData td) {
        return keyTransaction.equals(td.getBlameMetricName());
    }

    public void beforeSampling() {
        checkDeadThreads();
        releaseStackTraces();
        delegate.beforeSampling();
    }

    private void checkDeadThreads() {
        long currentTime = System.nanoTime();
        if (currentTime - lastThreadCheck > THREAD_CHECK_INTERVAL_IN_NANOS) {
            lastThreadCheck = currentTime;
            Set liveRequestThreads = ServiceFactory.getThreadService().getRequestThreadIds();
            List<Long> deadRequestThreads = new ArrayList(requestThreads);
            deadRequestThreads.removeAll(liveRequestThreads);
            for (Long threadId : deadRequestThreads) {
                removeHolderQueue(threadId.longValue());
            }
        }
    }

    public void addStackTrace(long threadId, boolean runnable, ThreadType type, StackTraceElement[] stackTrace) {
        if ((type != ThreadType.BasicThreadType.REQUEST) && (type != ThreadType.BasicThreadType.BACKGROUND)) {
            return;
        }
        StackTraceHolder holder = new StackTraceHolder(threadId, runnable, type, stackTrace);
        Queue holderQueue = getOrCreateHolderQueue(threadId);
        holderQueue.offer(holder);
    }

    private Queue<StackTraceHolder> getHolderQueue(long threadId) {
        return (Queue) pendingStackTraces.get(Long.valueOf(threadId));
    }

    private Queue<StackTraceHolder> getOrCreateHolderQueue(long threadId) {
        Queue holderQueue = (Queue) pendingStackTraces.get(Long.valueOf(threadId));
        if (holderQueue == null) {
            holderQueue = new LinkedBlockingQueue(100);
            pendingStackTraces.put(Long.valueOf(threadId), holderQueue);
            requestThreads.add(Long.valueOf(threadId));
        }
        return holderQueue;
    }

    private void removeHolderQueue(long threadId) {
        pendingStackTraces.remove(Long.valueOf(threadId));
        requestThreads.remove(Long.valueOf(threadId));
    }

    public void markInstrumentedMethods() {
    }

    private static class StackTraceHolder {
        private final long threadId;
        private final boolean runnable;
        private final ThreadType type;
        private final long stackTraceTime;
        private final StackTraceElement[] stackTrace;

        private StackTraceHolder(long threadId, boolean runnable, ThreadType type, StackTraceElement[] stackTrace) {
            this.threadId = threadId;
            this.runnable = runnable;
            this.type = type;
            this.stackTrace = stackTrace;
            stackTraceTime = System.nanoTime();
        }

        public long getThreadId() {
            return threadId;
        }

        public boolean isRunnable() {
            return runnable;
        }

        public ThreadType getType() {
            return type;
        }

        public StackTraceElement[] getStackTrace() {
            return stackTrace;
        }

        public long getStackTraceTime() {
            return stackTraceTime;
        }
    }
}