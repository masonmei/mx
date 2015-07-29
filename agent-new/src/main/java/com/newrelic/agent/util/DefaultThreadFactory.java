package com.newrelic.agent.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.newrelic.agent.Agent;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.service.ServiceFactory;

public class DefaultThreadFactory implements ThreadFactory {
    private final String name;
    private final boolean daemon;
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    public DefaultThreadFactory(String name, boolean daemon) {
        this.name = name;
        this.daemon = daemon;
    }

    public Thread newThread(Runnable r) {
        int num = this.threadNumber.getAndIncrement();
        String threadName = this.name + " " + num;
        Thread t = new AgentThreadImpl(r, threadName);
        Agent.LOG.fine("Created agent thread: " + t.getName());
        ServiceFactory.getThreadService().registerAgentThreadId(t.getId());
        if (this.daemon) {
            t.setDaemon(true);
        }
        return t;
    }

    private static class AgentThreadImpl extends Thread implements ThreadService.AgentThread {
        public AgentThreadImpl(Runnable r, String threadName) {
            super(threadName);
        }
    }
}