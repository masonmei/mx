package com.newrelic.agent.deadlock;

import java.text.MessageFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.DefaultThreadFactory;
import com.newrelic.agent.util.SafeWrappers;

public class DeadlockDetectorService extends AbstractService {
    private static final String DEADLOCK_DETECTOR_THREAD_NAME = "New Relic Deadlock Detector";
    private static final long INITIAL_DELAY_IN_SECONDS = 300L;
    private static final long SUBSEQUENT_DELAY_IN_SECONDS = 300L;
    private final boolean isEnabled;
    private final ScheduledExecutorService scheduledExecutor;
    private volatile ScheduledFuture<?> deadlockTask;

    public DeadlockDetectorService() {
        super(DeadlockDetectorService.class.getSimpleName());
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        this.isEnabled = ((Boolean) config.getValue("deadlock_detector.enabled", Boolean.valueOf(true))).booleanValue();

        ThreadFactory threadFactory =
                this.isEnabled ? new DefaultThreadFactory("New Relic Deadlock Detector", true) : null;

        this.scheduledExecutor = (this.isEnabled ? Executors.newSingleThreadScheduledExecutor(threadFactory) : null);
    }

    protected void doStart() {
        if (!this.isEnabled) {
            return;
        }

        final DeadLockDetector deadlockDetector = getDeadlockDetector();
        try {
            deadlockDetector.detectDeadlockedThreads();
        } catch (Throwable t) {
            this.logger
                    .log(Level.FINE, t, "Failed to detect deadlocked threads: {0}.  The Deadlock detector is disabled.",
                                new Object[] {t.toString()});

            this.logger.log(Level.FINEST, t, t.toString(), new Object[0]);
            return;
        }
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    deadlockDetector.detectDeadlockedThreads();
                } catch (Throwable t) {
                    String msg = MessageFormat.format("Failed to detect deadlocked threads: {0}",
                                                             new Object[] {t.toString()});
                    if (DeadlockDetectorService.this.getLogger().isLoggable(Level.FINER)) {
                        DeadlockDetectorService.this.getLogger().log(Level.WARNING, msg, t);
                    } else {
                        DeadlockDetectorService.this.getLogger().warning(msg);
                    }
                }
            }
        };
        this.deadlockTask = this.scheduledExecutor
                                    .scheduleWithFixedDelay(SafeWrappers.safeRunnable(runnable), 300L, 300L,
                                                                   TimeUnit.SECONDS);
    }

    protected void doStop() {
        if (!this.isEnabled) {
            return;
        }
        if (this.deadlockTask != null) {
            this.deadlockTask.cancel(false);
        }
        this.scheduledExecutor.shutdown();
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    private DeadLockDetector getDeadlockDetector() {
        return new DeadLockDetector();
    }
}