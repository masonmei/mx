package com.newrelic.agent.reinstrument;

import java.lang.instrument.UnmodifiableClassException;
import java.text.MessageFormat;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.newrelic.agent.Agent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.deps.com.google.common.collect.Sets;

public class PeriodicRetransformer implements Runnable {
    public static final PeriodicRetransformer INSTANCE = new PeriodicRetransformer();
    private static final int FREQUENCY_IN_SECONDS = 10;
    private final AtomicReference<ConcurrentLinkedQueue<Class<?>>> classesToRetransform =
            new AtomicReference(new ConcurrentLinkedQueue());
    private final AtomicBoolean scheduled = new AtomicBoolean(false);

    public void run() {
        ConcurrentLinkedQueue classList =
                (ConcurrentLinkedQueue) this.classesToRetransform.getAndSet(new ConcurrentLinkedQueue());
        if (classList.isEmpty()) {
            return;
        }
        Set classSet = Sets.newHashSet(classList);
        try {
            ServiceFactory.getAgent().getInstrumentation().retransformClasses((Class[]) classSet.toArray(new Class[0]));
        } catch (UnmodifiableClassException e) {
            Agent.LOG.fine(MessageFormat.format("Unable to retransform class: {0}", new Object[] {e.getMessage()}));
        }
    }

    public void queueRetransform(Class<?> classToRetransform) {
        ((ConcurrentLinkedQueue) this.classesToRetransform.get()).add(classToRetransform);
        if ((!this.scheduled.get()) && (!this.scheduled.getAndSet(true))) {
            ServiceFactory.getSamplerService().addSampler(this, 10L, TimeUnit.SECONDS);
        }
    }
}