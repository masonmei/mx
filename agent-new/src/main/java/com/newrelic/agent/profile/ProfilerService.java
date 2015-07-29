package com.newrelic.agent.profile;

import com.newrelic.agent.HarvestService;
import com.newrelic.agent.commands.Command;
import com.newrelic.agent.commands.CommandParser;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ThreadProfilerConfig;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.DefaultThreadFactory;
import com.newrelic.agent.util.SafeWrappers;
import com.newrelic.agent.xray.IXRaySessionService;
import java.text.MessageFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class ProfilerService extends AbstractService
  implements ProfilerControl
{
  private static final String PROFILER_THREAD_NAME = "New Relic Profiler Service";
  private ProfileSession currentSession;
  private final XrayProfileSession xrayProfileSession;
  private final ScheduledExecutorService scheduledExecutor;

  public ProfilerService()
  {
    super(ProfilerService.class.getSimpleName());
    ThreadFactory threadFactory = new DefaultThreadFactory("New Relic Profiler Service", true);
    this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
    this.xrayProfileSession = new XrayProfileSession();
  }

  public boolean isEnabled()
  {
    return ServiceFactory.getConfigService().getDefaultAgentConfig().getThreadProfilerConfig().isEnabled();
  }

  public synchronized void startProfiler(ProfilerParameters parameters)
  {
    long samplePeriodInMillis = parameters.getSamplePeriodInMillis().longValue();
    long durationInMillis = parameters.getDurationInMillis().longValue();
    boolean enabled = ServiceFactory.getConfigService().getDefaultAgentConfig().getThreadProfilerConfig().isEnabled();
    if ((!enabled) || (samplePeriodInMillis <= 0L) || (durationInMillis <= 0L) || (samplePeriodInMillis > durationInMillis)) {
      getLogger().info(MessageFormat.format("Ignoring the start profiler command: enabled={0}, samplePeriodInMillis={1}, durationInMillis={2}", new Object[] { Boolean.valueOf(enabled), Long.valueOf(samplePeriodInMillis), Long.valueOf(durationInMillis) }));

      return;
    }

    ProfileSession oldSession = this.currentSession;
    if ((oldSession != null) && (!oldSession.isDone())) {
      getLogger().info(MessageFormat.format("Ignoring the start profiler command because a session is currently active. {0}", new Object[] { oldSession.getProfileId() }));

      return;
    }
    this.xrayProfileSession.suspend();
    ProfileSession newSession = createProfileSession(parameters);
    newSession.start();
    this.currentSession = newSession;
  }

  public synchronized int stopProfiler(Long profileId, boolean shouldReport)
  {
    ProfileSession session = this.currentSession;
    if ((session != null) && (profileId.equals(session.getProfileId()))) {
      session.stop(shouldReport);
      return 0;
    }
    return -1;
  }

  synchronized void sessionCompleted(ProfileSession session) {
    if (this.currentSession != session) {
      return;
    }
    this.currentSession = null;
  }

  protected ProfileSession createProfileSession(ProfilerParameters parameters) {
    return new ProfileSession(this, parameters);
  }

  protected ScheduledExecutorService getScheduledExecutorService() {
    return SafeWrappers.safeExecutor(this.scheduledExecutor);
  }

  protected void doStart()
  {
    addCommands();
    if (isEnabled()) {
      ServiceFactory.getHarvestService().addHarvestListener(this.xrayProfileSession);
      ServiceFactory.getXRaySessionService().addListener(this.xrayProfileSession);
    }
  }

  protected synchronized ProfileSession getCurrentSession() {
    return this.currentSession;
  }

  private void addCommands() {
    ServiceFactory.getCommandParser().addCommands(new Command[] { new StartProfilerCommand(this) });
    ServiceFactory.getCommandParser().addCommands(new Command[] { new StopProfilerCommand(this) });
  }

  protected void doStop()
  {
    if (isEnabled()) {
      ServiceFactory.getHarvestService().removeHarvestListener(this.xrayProfileSession);
      ServiceFactory.getXRaySessionService().removeListener(this.xrayProfileSession);
    }
    ProfileSession session = getCurrentSession();
    if (session != null) {
      session.stop(false);
    }

    if (this.scheduledExecutor != null) {
      this.scheduledExecutor.shutdown();
      try {
        if (!this.scheduledExecutor.awaitTermination(30L, TimeUnit.SECONDS))
          getLogger().log(Level.FINE, "Profiler Service executor service did not terminate");
      }
      catch (InterruptedException e)
      {
        Thread.currentThread().interrupt();
      }
    }
  }
}