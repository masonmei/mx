package com.newrelic.agent.util;

import com.newrelic.agent.Agent;
import com.newrelic.agent.logging.IAgentLogger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;

public class LatchingRunnable
  implements Runnable
{
  final CountDownLatch latch = new CountDownLatch(1);

  public void run()
  {
    this.latch.countDown();
  }

  public void block() {
    try {
      this.latch.await();
    } catch (InterruptedException e) {
      Agent.LOG.log(Level.FINE, "Latch error", e);
    }
  }

  public static void drain(Executor executor)
  {
    LatchingRunnable runnable = new LatchingRunnable();
    try {
      executor.execute(runnable);
      runnable.block();
    } catch (RejectedExecutionException e) {
      Agent.LOG.finest("Unable to drain executor");
    }
  }
}