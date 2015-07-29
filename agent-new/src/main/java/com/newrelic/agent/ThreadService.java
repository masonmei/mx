package com.newrelic.agent;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.util.DefaultThreadFactory;
import com.newrelic.agent.util.SafeWrappers;

public class ThreadService extends AbstractService {
    private static final float HASH_SET_LOAD_FACTOR = 0.75F;
    private static final String THREAD_SERVICE_THREAD_NAME = "New Relic Thread Service";
    private static final long INITIAL_DELAY_IN_SECONDS = 300L;
    private static final long SUBSEQUENT_DELAY_IN_SECONDS = 300L;
    private final Map<Long, Boolean> agentThreadIds;
    private final Map<Long, Boolean> requestThreadIds;
    private final Map<Long, Boolean> backgroundThreadIds;
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private volatile ScheduledExecutorService scheduledExecutor;
    private volatile ScheduledFuture<?> deadThreadsTask;

    public ThreadService() {
        super(ThreadService.class.getSimpleName());
        this.agentThreadIds = new ConcurrentHashMap(6);
        this.requestThreadIds = new ConcurrentHashMap();
        this.backgroundThreadIds = new ConcurrentHashMap();
    }

    protected void doStart() {
        if (this.threadMXBean == null) {
            return;
        }
        ThreadFactory threadFactory = new DefaultThreadFactory("New Relic Thread Service", true);
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    ThreadService.this.detectDeadThreads();
                } catch (Throwable t) {
                    String msg = MessageFormat.format("Unexpected exception detecting dead threads: {0}",
                                                             new Object[] {t.toString()});
                    ThreadService.this.getLogger().warning(msg);
                }
            }
        };
        this.deadThreadsTask = this.scheduledExecutor
                                       .scheduleWithFixedDelay(SafeWrappers.safeRunnable(runnable), 300L, 300L,
                                                                      TimeUnit.SECONDS);
    }

    protected void doStop() {
        if (this.deadThreadsTask != null) {
            this.deadThreadsTask.cancel(false);
        }
        this.scheduledExecutor.shutdown();
    }

    protected void detectDeadThreads() {
        long[] threadIds = this.threadMXBean.getAllThreadIds();
        int hashSetSize = (int) (threadIds.length / 0.75F) + 1;
        Set ids = new HashSet(hashSetSize);
        for (long threadId : threadIds) {
            ids.add(Long.valueOf(threadId));
        }
        retainAll(this.requestThreadIds, ids);
        retainAll(this.backgroundThreadIds, ids);
    }

    private void retainAll(Map<Long, Boolean> map, Set<Long> ids) {
        for (Entry entry : map.entrySet()) {
            if (!ids.contains(entry.getKey())) {
                map.remove(entry.getKey());
            }
        }
    }

    public Set<Long> getRequestThreadIds() {
        return Collections.unmodifiableSet(this.requestThreadIds.keySet());
    }

    public Set<Long> getBackgroundThreadIds() {
        return Collections.unmodifiableSet(this.backgroundThreadIds.keySet());
    }

    public void noticeRequestThread(Long threadId) {
        this.requestThreadIds.put(threadId, Boolean.TRUE);
    }

    public void noticeBackgroundThread(Long threadId) {
        this.backgroundThreadIds.put(threadId, Boolean.TRUE);
    }

    public boolean isEnabled() {
        return true;
    }

    public boolean isCurrentThreadAnAgentThread() {
        return Thread.currentThread() instanceof AgentThread;
    }

    public boolean isAgentThreadId(Long threadId) {
        return this.agentThreadIds.containsKey(threadId);
    }

    public Set<Long> getAgentThreadIds() {
        return Collections.unmodifiableSet(this.agentThreadIds.keySet());
    }

    public void registerAgentThreadId(long id) {
        this.agentThreadIds.put(Long.valueOf(id), Boolean.TRUE);
    }

    public static abstract interface AgentThread {
    }
}