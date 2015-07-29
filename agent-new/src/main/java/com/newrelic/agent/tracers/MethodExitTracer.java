package com.newrelic.agent.tracers;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.logging.IAgentLogger;
import java.text.MessageFormat;
import java.util.logging.Level;

public abstract class MethodExitTracer extends MethodExitTracerNoSkip
  implements SkipTracer
{
  public MethodExitTracer(ClassMethodSignature signature, Transaction transaction)
  {
    super(signature, transaction);
  }

  public final void childTracerFinished(Tracer child)
  {
    if (Agent.LOG.isLoggable(Level.FINER)) {
      String msg = MessageFormat.format("childTracerFinished called on {0} with child {1}", new Object[] { getClass().getName(), child.getClass().getName() });

      Agent.LOG.finer(msg);
    }
  }
}