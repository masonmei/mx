package com.newrelic.agent.rpm;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import com.newrelic.agent.IRPMService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.environment.Environment;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.DefaultThreadFactory;
import com.newrelic.agent.util.SafeWrappers;

public class RPMConnectionServiceImpl extends AbstractService implements RPMConnectionService {
  public static final String RPM_CONNECTION_THREAD_NAME = "New Relic RPM Connection Service";
  public static final long INITIAL_APP_SERVER_PORT_DELAY = 5L;
  public static final long SUBSEQUENT_APP_SERVER_PORT_DELAY = 5L;
  public static final long APP_SERVER_PORT_TIMEOUT = 120L;
  public static final long CONNECT_ATTEMPT_INTERVAL = 60L;
  private final ScheduledExecutorService scheduledExecutor;

  public RPMConnectionServiceImpl() {
    super(RPMConnectionService.class.getSimpleName());
    ThreadFactory threadFactory = new DefaultThreadFactory("New Relic RPM Connection Service", true);
    scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
  }

  protected void doStart() {
  }

  protected void doStop() {
    scheduledExecutor.shutdown();
  }

  public void connect(IRPMService rpmService) {
    RPMConnectionTask connectionTask = new RPMConnectionTask(rpmService);
    connectionTask.start();
  }

  public void connectImmediate(IRPMService rpmService) {
    RPMConnectionTask connectionTask = new RPMConnectionTask(rpmService);
    connectionTask.startImmediate();
  }

  public boolean isEnabled() {
    return true;
  }

  public long getInitialAppServerPortDelay() {
    return 5L;
  }

  public long getAppServerPortTimeout() {
    return 120L;
  }

  private final class RPMConnectionTask implements Runnable {
    private final IRPMService rpmService;
    private final AtomicReference<ScheduledFuture<?>> appServerPortTask = new AtomicReference();
    private final AtomicReference<ScheduledFuture<?>> appServerPortTimeoutTask = new AtomicReference();
    private final AtomicReference<ScheduledFuture<?>> connectTask = new AtomicReference();
    private final AtomicBoolean connectTaskStarted = new AtomicBoolean();

    private RPMConnectionTask(IRPMService rpmService) {
      this.rpmService = rpmService;
    }

    public void run() {
    }

    private void start() {
      if (!rpmService.isMainApp()) {
        startImmediate();
      } else if (isSyncStartup()) {
        getLogger().log(Level.FINER, "Not waiting for application server port");
        startSync();
      } else {
        getLogger().log(Level.FINER, "Waiting for application server port");
        appServerPortTask.set(scheduleAppServerPortTask());
        appServerPortTimeoutTask.set(scheduleAppServerPortTimeoutTask());
      }
    }

    private void startSync() {
      if ((isConnected()) || (attemptConnection())) {
        return;
      }
      startImmediate();
    }

    private void startImmediate() {
      connectTask.set(scheduleConnectTask());
    }

    private void stop() {
      getLogger().log(Level.FINER, "Stopping New Relic connection task for {0}",
                             new Object[] {rpmService.getApplicationName()});
      ScheduledFuture handle = (ScheduledFuture) appServerPortTask.get();
      if (handle != null) {
        handle.cancel(false);
      }
      handle = (ScheduledFuture) connectTask.get();
      if (handle != null) {
        handle.cancel(false);
      }
      handle = (ScheduledFuture) appServerPortTimeoutTask.get();
      if (handle != null) {
        handle.cancel(false);
      }
    }

    private ScheduledFuture<?> scheduleAppServerPortTask() {
      return scheduledExecutor.scheduleWithFixedDelay(SafeWrappers.safeRunnable(new Runnable() {
        public void run() {
          if (RPMConnectionTask.this.isConnected()) {
            RPMConnectionTask.this.stop();
            return;
          }
          if ((RPMConnectionTask.this.hasAppServerPort()) && (!RPMConnectionTask.this.connectTaskStarted())) {
            RPMConnectionTask.this.stop();
            getLogger().log(Level.FINER, "Discovered application server port");
            connectTask.set(RPMConnectionTask.this.scheduleConnectTask());
          }
        }
      }), getInitialAppServerPortDelay(), 5L, TimeUnit.SECONDS);
    }

    private ScheduledFuture<?> scheduleAppServerPortTimeoutTask() {
      return scheduledExecutor.schedule(SafeWrappers.safeRunnable(new Runnable() {
        public void run() {
          if (!RPMConnectionTask.this.connectTaskStarted()) {
            RPMConnectionTask.this.stop();
            if (!RPMConnectionTask.this.isConnected()) {
              if (!RPMConnectionTask.this.hasAppServerPort()) {
                getLogger().log(Level.FINER, "Gave up waiting for application server port");
              }
              connectTask.set(RPMConnectionTask.this.scheduleConnectTask());
            }
          }
        }
      }), getAppServerPortTimeout(), TimeUnit.SECONDS);
    }

    private ScheduledFuture<?> scheduleConnectTask() {
      return scheduledExecutor.scheduleWithFixedDelay(SafeWrappers.safeRunnable(new Runnable() {
        public void run() {
          if ((RPMConnectionTask.this.shouldAttemptConnection()) && (RPMConnectionTask.this
                                                                             .attemptConnection())) {
            RPMConnectionTask.this.stop();
          }
        }
      }), 0L, 60L, TimeUnit.SECONDS);
    }

    private boolean isMainAppConnected() {
      return ServiceFactory.getRPMService().isConnected();
    }

    private boolean isConnected() {
      return rpmService.isConnected();
    }

    private boolean connectTaskStarted() {
      return connectTaskStarted.getAndSet(true);
    }

    private boolean hasAppServerPort() {
      return getEnvironment().getAgentIdentity().getServerPort() != null;
    }

    private boolean isSyncStartup() {
      ConfigService configService = ServiceFactory.getConfigService();
      AgentConfig config = configService.getAgentConfig(rpmService.getApplicationName());
      return config.isSyncStartup();
    }

    private boolean shouldAttemptConnection() {
      if ((rpmService.isMainApp()) || (isMainAppConnected())) {
        return !isConnected();
      }
      return false;
    }

    private boolean attemptConnection() {
      try {
        rpmService.launch();
        return true;
      } catch (Throwable e) {
        getLogger().log(Level.INFO, "Failed to connect to {0} for {1}: {2}",
                               new Object[] {rpmService.getHostString(), rpmService.getApplicationName(),
                                                    e.toString()});

        getLogger().log(Level.FINEST, e, e.toString(), new Object[0]);
      }
      return false;
    }

    private Environment getEnvironment() {
      return ServiceFactory.getEnvironmentService().getEnvironment();
    }
  }
}