//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.profile;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.newrelic.agent.Agent;
import com.newrelic.agent.profile.ThreadType.BasicThreadType;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.StackTraces;

public class ProfileSampler {
    public static final int MAX_STACK_DEPTH = 300;
    private static final ThreadInfo[] EMPTY_THREAD_INFO_ARRAY = new ThreadInfo[0];

    public ProfileSampler() {
    }

    public void sampleStackTraces(List<IProfile> profiles) {
        if (!profiles.isEmpty()) {
            Set runningThreadIds = this.getRunningThreadIds();
            Set requestThreadIds = this.getRequestThreadIds(runningThreadIds);
            ThreadInfo[] requestThreadInfos = null;
            Iterator i$ = profiles.iterator();

            while (i$.hasNext()) {
                IProfile profile = (IProfile) i$.next();
                profile.beforeSampling();
                if (profile.getProfilerParameters().isOnlyRequestThreads()) {
                    if (requestThreadInfos == null) {
                        requestThreadInfos = this.getRequestThreadInfos(requestThreadIds);
                    }

                    this.addRequestInfos(profile, requestThreadInfos);
                } else {
                    this.addThreadInfos(profile, runningThreadIds, requestThreadIds, this.getAllThreadInfos());
                }
            }

        }
    }

    private void addRequestInfos(IProfile profile, ThreadInfo[] threadInfos) {
        RunnableThreadRules runnableThreadRules = new RunnableThreadRules();
        ThreadInfo[] arr$ = threadInfos;
        int len$ = threadInfos.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            ThreadInfo threadInfo = arr$[i$];
            if (threadInfo != null) {
                boolean isRunnable = runnableThreadRules.isRunnable(threadInfo);
                profile.addStackTrace(threadInfo.getThreadId(), isRunnable, BasicThreadType.REQUEST,
                                             threadInfo.getStackTrace());
            }
        }

    }

    private void addThreadInfos(IProfile profiler, Set<Long> runningThreadIds, Set<Long> requestThreadIds,
                                ThreadInfo[] threadInfos) {
        if (threadInfos.length != 0) {
            Set backgroundThreadIds = this.getBackgroundThreadIds(runningThreadIds);
            Set agentThreadIds = ServiceFactory.getThreadService().getAgentThreadIds();
            RunnableThreadRules runnableThreadRules = new RunnableThreadRules();
            ThreadInfo[] arr$ = threadInfos;
            int len$ = threadInfos.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                ThreadInfo threadInfo = arr$[i$];
                if (threadInfo != null) {
                    boolean isRunnable = runnableThreadRules.isRunnable(threadInfo);
                    if (isRunnable || !profiler.getProfilerParameters().isRunnablesOnly()) {
                        long threadId = threadInfo.getThreadId();
                        BasicThreadType type;
                        if (agentThreadIds.contains(Long.valueOf(threadId))) {
                            type = BasicThreadType.AGENT;
                        } else if (profiler.getProfilerParameters().isProfileAgentThreads() && StackTraces
                                                                                                       .isInAgentInstrumentation(threadInfo
                                                                                                                                         .getStackTrace())) {
                            type = BasicThreadType.AGENT_INSTRUMENTATION;
                        } else if (requestThreadIds.contains(Long.valueOf(threadId))) {
                            type = BasicThreadType.REQUEST;
                        } else if (backgroundThreadIds.contains(Long.valueOf(threadId))) {
                            type = BasicThreadType.BACKGROUND;
                        } else {
                            type = BasicThreadType.OTHER;
                        }

                        profiler.addStackTrace(threadId, isRunnable, type, threadInfo.getStackTrace());
                    }
                }
            }

        }
    }

    private Set<Long> getRunningThreadIds() {
        return ServiceFactory.getTransactionService().getRunningThreadIds();
    }

    private Set<Long> getRequestThreadIds(Set<Long> runningThreadIds) {
        HashSet result = new HashSet(ServiceFactory.getThreadService().getRequestThreadIds());
        result.retainAll(runningThreadIds);
        return result;
    }

    private Set<Long> getBackgroundThreadIds(Set<Long> runningThreadIds) {
        HashSet result = new HashSet(ServiceFactory.getThreadService().getBackgroundThreadIds());
        result.retainAll(runningThreadIds);
        return result;
    }

    private ThreadInfo[] getAllThreadInfos() {
        long[] threadIds = this.getAllThreadIds();
        if (threadIds != null && threadIds.length != 0) {
            HashSet ids = new HashSet(threadIds.length);
            long[] arr$ = threadIds;
            int len$ = threadIds.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                long threadId = arr$[i$];
                ids.add(Long.valueOf(threadId));
            }

            ids.remove(Long.valueOf(Thread.currentThread().getId()));
            threadIds = this.convertToLongArray(ids);
            return this.getThreadInfos(threadIds);
        } else {
            return EMPTY_THREAD_INFO_ARRAY;
        }
    }

    private ThreadInfo[] getRequestThreadInfos(Set<Long> requestThreadIds) {
        long[] threadIds = this.convertToLongArray(requestThreadIds);
        return this.getThreadInfos(threadIds);
    }

    private long[] convertToLongArray(Set<Long> ids) {
        long[] arr = new long[ids.size()];
        int i = 0;

        Long id;
        for (Iterator i$ = ids.iterator(); i$.hasNext(); arr[i++] = id.longValue()) {
            id = (Long) i$.next();
        }

        return arr;
    }

    private ThreadInfo[] getThreadInfos(long[] threadIds) {
        try {
            ThreadMXBean e = ManagementFactory.getThreadMXBean();
            if (threadIds.length > 0) {
                return e.getThreadInfo(threadIds, 300);
            }
        } catch (SecurityException var3) {
            Agent.LOG.finer(MessageFormat.format("An error occurred getting thread info: {0}", new Object[] {var3}));
        }

        return EMPTY_THREAD_INFO_ARRAY;
    }

    private long[] getAllThreadIds() {
        try {
            ThreadMXBean e = ManagementFactory.getThreadMXBean();
            return e.getAllThreadIds();
        } catch (SecurityException var2) {
            Agent.LOG.finer(MessageFormat.format("An error occurred getting all thread ids: {0}", new Object[] {var2}));
            return null;
        }
    }
}
