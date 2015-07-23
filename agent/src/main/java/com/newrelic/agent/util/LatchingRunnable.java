package com.newrelic.agent.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;

import com.newrelic.agent.Agent;

public class LatchingRunnable implements Runnable {
    final CountDownLatch latch = new CountDownLatch(1);

    public static void drain(Executor executor) {
        LatchingRunnable runnable = new LatchingRunnable();
        try {
            executor.execute(runnable);
            runnable.block();
        } catch (RejectedExecutionException e) {
            Agent.LOG.finest("Unable to drain executor");
        }
    }

    public void run() {
        latch.countDown();
    }

    public void block() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Agent.LOG.log(Level.FINE, "Latch error", e);
        }
    }
}