package com.newrelic.agent.profile;

import java.text.MessageFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.agent.commands.Command;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.DefaultThreadFactory;
import com.newrelic.agent.util.SafeWrappers;

public class ProfilerService extends AbstractService implements ProfilerControl {
    private static final String PROFILER_THREAD_NAME = "New Relic Profiler Service";
    private final XrayProfileSession xrayProfileSession;
    private final ScheduledExecutorService scheduledExecutor;
    private ProfileSession currentSession;

    public ProfilerService() {
        super(ProfilerService.class.getSimpleName());
        ThreadFactory threadFactory = new DefaultThreadFactory("New Relic Profiler Service", true);
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        xrayProfileSession = new XrayProfileSession();
    }

    public boolean isEnabled() {
        return ServiceFactory.getConfigService().getDefaultAgentConfig().getThreadProfilerConfig().isEnabled();
    }

    public synchronized void startProfiler(ProfilerParameters parameters) {
        long samplePeriodInMillis = parameters.getSamplePeriodInMillis().longValue();
        long durationInMillis = parameters.getDurationInMillis().longValue();
        boolean enabled =
                ServiceFactory.getConfigService().getDefaultAgentConfig().getThreadProfilerConfig().isEnabled();
        if ((!enabled) || (samplePeriodInMillis <= 0L) || (durationInMillis <= 0L) || (samplePeriodInMillis
                                                                                               > durationInMillis)) {
            getLogger().info(MessageFormat.format("Ignoring the start profiler command: enabled={0}, "
                                                          + "samplePeriodInMillis={1}, durationInMillis={2}",
                                                         new Object[] {Boolean.valueOf(enabled),
                                                                              Long.valueOf(samplePeriodInMillis),
                                                                              Long.valueOf(durationInMillis)}));

            return;
        }

        ProfileSession oldSession = currentSession;
        if ((oldSession != null) && (!oldSession.isDone())) {
            getLogger().info(MessageFormat.format("Ignoring the start profiler command because a session is currently "
                                                          + "active. {0}", new Object[] {oldSession.getProfileId()}));

            return;
        }
        xrayProfileSession.suspend();
        ProfileSession newSession = createProfileSession(parameters);
        newSession.start();
        currentSession = newSession;
    }

    public synchronized int stopProfiler(Long profileId, boolean shouldReport) {
        ProfileSession session = currentSession;
        if ((session != null) && (profileId.equals(session.getProfileId()))) {
            session.stop(shouldReport);
            return 0;
        }
        return -1;
    }

    synchronized void sessionCompleted(ProfileSession session) {
        if (currentSession != session) {
            return;
        }
        currentSession = null;
    }

    protected ProfileSession createProfileSession(ProfilerParameters parameters) {
        return new ProfileSession(this, parameters);
    }

    protected ScheduledExecutorService getScheduledExecutorService() {
        return SafeWrappers.safeExecutor(scheduledExecutor);
    }

    protected void doStart() {
        addCommands();
        if (isEnabled()) {
            ServiceFactory.getHarvestService().addHarvestListener(xrayProfileSession);
            ServiceFactory.getXRaySessionService().addListener(xrayProfileSession);
        }
    }

    protected synchronized ProfileSession getCurrentSession() {
        return currentSession;
    }

    private void addCommands() {
        ServiceFactory.getCommandParser().addCommands(new Command[] {new StartProfilerCommand(this)});
        ServiceFactory.getCommandParser().addCommands(new Command[] {new StopProfilerCommand(this)});
    }

    protected void doStop() {
        if (isEnabled()) {
            ServiceFactory.getHarvestService().removeHarvestListener(xrayProfileSession);
            ServiceFactory.getXRaySessionService().removeListener(xrayProfileSession);
        }
        ProfileSession session = getCurrentSession();
        if (session != null) {
            session.stop(false);
        }

        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
            try {
                if (!scheduledExecutor.awaitTermination(30L, TimeUnit.SECONDS)) {
                    getLogger().log(Level.FINE, "Profiler Service executor service did not terminate");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}