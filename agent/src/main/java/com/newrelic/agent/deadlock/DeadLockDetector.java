package com.newrelic.agent.deadlock;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.agent.Agent;
import com.newrelic.agent.errors.DeadlockTraceError;
import com.newrelic.agent.errors.ErrorService;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.service.ServiceFactory;

public class DeadLockDetector {
    private static final int MAX_THREAD_DEPTH = 300;
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    protected void detectDeadlockedThreads() {
        ThreadInfo[] threadInfos = getDeadlockedThreadInfos();
        if (threadInfos.length > 0) {
            Agent.LOG.info(MessageFormat.format("Detected {0} deadlocked threads",
                                                       new Object[] {Integer.valueOf(threadInfos.length)}));
            if (Agent.isDebugEnabled()) {
                boolean harvestThreadLocked = false;
                for (ThreadInfo threadInfo : threadInfos) {
                    if (threadInfo.getThreadName().equals("New Relic Harvest Service")) {
                        harvestThreadLocked = true;
                    }
                }
                if (harvestThreadLocked) {
                    Agent.LOG.severe("A harvest thread deadlock condition was detected");
                    return;
                }
            }
            reportDeadlocks(Arrays.asList(threadInfos));
        }
    }

    ThreadInfo[] getDeadlockedThreadInfos() {
        long[] deadlockedThreadIds = findDeadlockedThreads();
        if (deadlockedThreadIds == null) {
            return new ThreadInfo[0];
        }
        return threadMXBean.getThreadInfo(deadlockedThreadIds, 300);
    }

    protected ThreadMXBean getThreadMXBean() {
        return threadMXBean;
    }

    protected long[] findDeadlockedThreads() {
        try {
            return getThreadMXBean().findDeadlockedThreads();
        } catch (UnsupportedOperationException e) {
        }
        return getThreadMXBean().findMonitorDeadlockedThreads();
    }

    private void reportDeadlocks(List<ThreadInfo> deadThreads) {
        TracedError[] tracedErrors = getTracedErrors(deadThreads);
        getErrorService().reportErrors(tracedErrors);
    }

    private ErrorService getErrorService() {
        return ServiceFactory.getRPMService().getErrorService();
    }

    TracedError[] getTracedErrors(List<ThreadInfo> threadInfos) {
        Map idToThreads = new HashMap();
        for (ThreadInfo thread : threadInfos) {
            idToThreads.put(Long.valueOf(thread.getThreadId()), thread);
        }

        List errors = new ArrayList();
        Set skipIds = new HashSet();
        for (ThreadInfo thread : threadInfos) {
            if (!skipIds.contains(Long.valueOf(thread.getThreadId()))) {
                long otherId = thread.getLockOwnerId();
                skipIds.add(Long.valueOf(otherId));
                ThreadInfo otherThread = (ThreadInfo) idToThreads.get(Long.valueOf(otherId));

                Map parameters = Maps.newHashMapWithExpectedSize(4);
                parameters.put("jvm.thread_name", thread.getThreadName());
                Map stackTraces = new HashMap();
                stackTraces.put(thread.getThreadName(), thread.getStackTrace());
                if (otherThread != null) {
                    parameters.put("jvm.lock_thread_name", otherThread.getThreadName());
                    stackTraces.put(otherThread.getThreadName(), otherThread.getStackTrace());
                }
                errors.add(new DeadlockTraceError(null, thread, stackTraces, parameters));
            }
        }

        return (TracedError[]) errors.toArray(new TracedError[0]);
    }
}