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
        isEnabled = ((Boolean) config.getValue("deadlock_detector.enabled", Boolean.valueOf(true))).booleanValue();

        ThreadFactory threadFactory = isEnabled ? new DefaultThreadFactory("New Relic Deadlock Detector", true) : null;

        scheduledExecutor = (isEnabled ? Executors.newSingleThreadScheduledExecutor(threadFactory) : null);
    }

    protected void doStart() {
        if (!isEnabled) {
            return;
        }

        final DeadLockDetector deadlockDetector = getDeadlockDetector();
        try {
            deadlockDetector.detectDeadlockedThreads();
        } catch (Throwable t) {
            logger.log(Level.FINE, t, "Failed to detect deadlocked threads: {0}.  The Deadlock detector is disabled.",
                              new Object[] {t.toString()});

            logger.log(Level.FINEST, t, t.toString(), new Object[0]);
            return;
        }
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    deadlockDetector.detectDeadlockedThreads();
                } catch (Throwable t) {
                    String msg = MessageFormat.format("Failed to detect deadlocked threads: {0}",
                                                             new Object[] {t.toString()});
                    if (getLogger().isLoggable(Level.FINER)) {
                        getLogger().log(Level.WARNING, msg, t);
                    } else {
                        getLogger().warning(msg);
                    }
                }
            }
        };
        deadlockTask = scheduledExecutor.scheduleWithFixedDelay(SafeWrappers.safeRunnable(runnable), 300L, 300L,
                                                                       TimeUnit.SECONDS);
    }

    protected void doStop() {
        if (!isEnabled) {
            return;
        }
        if (deadlockTask != null) {
            deadlockTask.cancel(false);
        }
        scheduledExecutor.shutdown();
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    private DeadLockDetector getDeadlockDetector() {
        return new DeadLockDetector();
    }
}