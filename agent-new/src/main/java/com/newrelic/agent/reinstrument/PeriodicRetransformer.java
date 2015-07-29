package com.newrelic.agent.reinstrument;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IAgent;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.deps.com.google.common.collect.Sets;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.samplers.SamplerService;
import com.newrelic.agent.service.ServiceFactory;
import java.lang.instrument.UnmodifiableClassException;
import java.text.MessageFormat;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PeriodicRetransformer
  implements Runnable
{
  private static final int FREQUENCY_IN_SECONDS = 10;
  private final AtomicReference<ConcurrentLinkedQueue<Class<?>>> classesToRetransform = new AtomicReference(new ConcurrentLinkedQueue());

  private final AtomicBoolean scheduled = new AtomicBoolean(false);

  public static final PeriodicRetransformer INSTANCE = new PeriodicRetransformer();

  public void run()
  {
    ConcurrentLinkedQueue classList = (ConcurrentLinkedQueue)this.classesToRetransform.getAndSet(new ConcurrentLinkedQueue());
    if (classList.isEmpty()) {
      return;
    }
    Set classSet = Sets.newHashSet(classList);
    try {
      ServiceFactory.getAgent().getInstrumentation().retransformClasses((Class[])classSet.toArray(new Class[0]));
    } catch (UnmodifiableClassException e) {
      Agent.LOG.fine(MessageFormat.format("Unable to retransform class: {0}", new Object[] { e.getMessage() }));
    }
  }

  public void queueRetransform(Class<?> classToRetransform) {
    ((ConcurrentLinkedQueue)this.classesToRetransform.get()).add(classToRetransform);
    if ((!this.scheduled.get()) && (!this.scheduled.getAndSet(true)))
      ServiceFactory.getSamplerService().addSampler(this, 10L, TimeUnit.SECONDS);
  }
}