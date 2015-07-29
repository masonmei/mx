package com.newrelic.agent.util;

import java.util.concurrent.Executor;

public class SynchronousExecutor
  implements Executor
{
  public void execute(Runnable command)
  {
    command.run();
  }
}